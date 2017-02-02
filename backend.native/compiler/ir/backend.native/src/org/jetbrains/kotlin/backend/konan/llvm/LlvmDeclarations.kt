package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny

internal fun createLlvmDeclarations(context: Context): LlvmDeclarations {
    val generator = DeclarationsGeneratorVisitor(context)
    context.ir.irModule.acceptChildrenVoid(generator)
    return with(generator) {
        LlvmDeclarations(
                functions, classes, fields, staticFields, theUnitInstanceRef
        )
    }

}

internal class LlvmDeclarations(
        private val functions: Map<FunctionDescriptor, FunctionLlvmDeclarations>,
        private val classes: Map<ClassDescriptor, ClassLlvmDeclarations>,
        private val fields: Map<PropertyDescriptor, FieldLlvmDeclarations>,
        private val staticFields: Map<PropertyDescriptor, StaticFieldLlvmDeclarations>,
        private val theUnitInstanceRef: ConstPointer?
) {
    fun forFunction(descriptor: FunctionDescriptor) = functions[descriptor] ?:
            error(descriptor.toString())

    fun forClass(descriptor: ClassDescriptor) = classes[descriptor] ?:
            error(descriptor.toString())

    fun forField(descriptor: PropertyDescriptor) = fields[descriptor] ?:
            error(descriptor.toString())

    fun forStaticField(descriptor: PropertyDescriptor) = staticFields[descriptor] ?:
            error(descriptor.toString())

    fun forSingleton(descriptor: ClassDescriptor) = forClass(descriptor).singletonDeclarations ?:
            error(descriptor.toString())

    fun getUnitInstanceRef() = theUnitInstanceRef ?: error("")

}

internal class ClassLlvmDeclarations(
        val bodyType: LLVMTypeRef,
        val fields: List<PropertyDescriptor>, // TODO: it is not an LLVM declaration.
        val typeInfoGlobal: StaticData.Global,
        val typeInfo: ConstPointer,
        val singletonDeclarations: SingletonLlvmDeclarations?)

internal class SingletonLlvmDeclarations(val instanceFieldRef: LLVMValueRef)

internal class FunctionLlvmDeclarations(val llvmFunction: LLVMValueRef)

internal class FieldLlvmDeclarations(val index: Int, val classBodyType: LLVMTypeRef)

internal class StaticFieldLlvmDeclarations(val storage: LLVMValueRef)

// TODO: rework getFields and getDeclaredFields.

/**
 * All fields of the class instance.
 * The order respects the class hierarchy, i.e. a class [fields] contains superclass [fields] as a prefix.
 */
private fun getFields(context: Context, classDescriptor: ClassDescriptor): List<PropertyDescriptor> {
    val superClass = classDescriptor.getSuperClassNotAny() // TODO: what if Any has fields?
    val superFields = if (superClass != null) getFields(context, superClass) else emptyList()

    return superFields + getDeclaredFields(context, classDescriptor)
}

/**
 * Fields declared in the class.
 */
private fun getDeclaredFields(context: Context, classDescriptor: ClassDescriptor): List<PropertyDescriptor> {
    // TODO: Here's what is going on here:
    // The existence of a backing field for a property is only described in the IR,
    // but not in the property descriptor.
    // That works, while we process the IR, but not for deserialized descriptors.
    //
    // So to have something in deserialized descriptors,
    // while we still see the IR, we mark the property with an annotation.
    //
    // We could apply the annotation during IR rewite, but we still are not
    // that far in the rewriting infrastructure. So we postpone
    // the annotation until the serializer.
    //
    // In this function we check the presence of the backing filed
    // two ways: first we check IR, then we check the annotation.

    val irClass = context.ir.moduleIndexForCodegen.classes[classDescriptor]
    if (irClass != null) {
        val declarations = irClass.declarations

        return declarations.mapNotNull {
            when (it) {
                is IrProperty -> it.backingField?.descriptor
                is IrField -> it.descriptor
                else -> null
            }
        }
    } else {
        val properties = classDescriptor.unsubstitutedMemberScope.
                getContributedDescriptors().
                filterIsInstance<PropertyDescriptor>()

        return properties.mapNotNull { it.backingField }
    }
}

private fun ContextUtils.createClassBodyType(name: String, fields: List<PropertyDescriptor>): LLVMTypeRef {
    val fieldTypes = fields.map { getLLVMType(it.type) }.toTypedArray()

    val classType = LLVMStructCreateNamed(LLVMGetModuleContext(context.llvmModule), name)!!

    memScoped {
        val fieldTypesNativeArrayPtr = allocArrayOf(*fieldTypes)[0].ptr
        LLVMStructSetBody(classType, fieldTypesNativeArrayPtr, fieldTypes.size, 0)
    }
    return classType
}

private class DeclarationsGeneratorVisitor(override val context: Context) :
        IrElementVisitorVoid, ContextUtils {

    val functions = mutableMapOf<FunctionDescriptor, FunctionLlvmDeclarations>()
    val classes = mutableMapOf<ClassDescriptor, ClassLlvmDeclarations>()
    val fields = mutableMapOf<PropertyDescriptor, FieldLlvmDeclarations>()
    val staticFields = mutableMapOf<PropertyDescriptor, StaticFieldLlvmDeclarations>()
    var theUnitInstanceRef: ConstPointer? = null

    private class Namer(val prefix: String) {
        private val names = mutableMapOf<DeclarationDescriptor, Name>()
        private val counts = mutableMapOf<FqName, Int>()

        fun getName(parent: FqName, descriptor: DeclarationDescriptor): Name {
            return names.getOrPut(descriptor) {
                val count = counts.getOrDefault(parent, 0) + 1
                counts[parent] = count
                Name.identifier(prefix + count)
            }
        }
    }

    val objectNamer = Namer("object-")

    private fun getLocalName(parent: FqName, descriptor: DeclarationDescriptor): Name {
        if (DescriptorUtils.isAnonymousObject(descriptor)) {
            return objectNamer.getName(parent, descriptor)
        }

        return descriptor.name
    }

    private fun getFqName(descriptor: DeclarationDescriptor): FqName {
        if (descriptor is PackageFragmentDescriptor) {
            return descriptor.fqName
        }


        val containingDeclaration = descriptor.containingDeclaration
        val parent = if (containingDeclaration != null) {
            getFqName(containingDeclaration)
        } else {
            FqName.ROOT
        }

        val localName = getLocalName(parent, descriptor)
        return parent.child(localName)
    }

    /**
     * Produces the name to be used for non-exported LLVM declarations corresponding to [descriptor].
     *
     * Note: since these declarations are going to be private, the name is only required not to clash with any
     * exported declarations.
     */
    private fun qualifyInternalName(descriptor: DeclarationDescriptor): String {
        return getFqName(descriptor).asString() + "#internal"
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {

        if (declaration.descriptor.isIntrinsic) {
            // do not generate any declarations for intrinsic classes as they require special handling
        } else {
            this.classes[declaration.descriptor] = createClassDeclarations(declaration)
        }

        super.visitClass(declaration)
    }

    private fun createClassDeclarations(declaration: IrClass): ClassLlvmDeclarations {
        val descriptor = declaration.descriptor

        val internalName = qualifyInternalName(descriptor)

        val fields = getFields(context, descriptor)
        val bodyType = createClassBodyType("kclassbody:$internalName", fields)

        val typeInfoPtr: ConstPointer
        val typeInfoGlobal: StaticData.Global

        val typeInfoSymbolName = if (descriptor.isExported()) {
            descriptor.typeInfoSymbolName
        } else {
            "ktype:$internalName"
        }

        if (!descriptor.isAbstract()) {
            // Create the special global consisting of TypeInfo and vtable.

            val typeInfoGlobalName = "ktypeglobal:$internalName"

            val typeInfoWithVtableType = structType(
                    runtime.typeInfoType,
                    LLVMArrayType(int8TypePtr, descriptor.vtableEntries.size)!!
            )

            typeInfoGlobal = staticData.createGlobal(typeInfoWithVtableType, typeInfoGlobalName, isExported = false)

            val llvmTypeInfoPtr = LLVMAddAlias(context.llvmModule,
                    kTypeInfoPtr,
                    typeInfoGlobal.pointer.getElementPtr(0).llvm,
                    typeInfoSymbolName)!!

            if (!descriptor.isExported()) {
                LLVMSetLinkage(llvmTypeInfoPtr, LLVMLinkage.LLVMInternalLinkage)
            }

            typeInfoPtr = constPointer(llvmTypeInfoPtr)

        } else {
            typeInfoGlobal = staticData.createGlobal(runtime.typeInfoType,
                    typeInfoSymbolName,
                    isExported = descriptor.isExported())

            typeInfoPtr = typeInfoGlobal.pointer
        }

        val singletonDeclarations = if (descriptor.kind.isSingleton) {
            createSingletonDeclarations(descriptor, typeInfoPtr, bodyType)
        } else {
            null
        }

        return ClassLlvmDeclarations(bodyType, fields, typeInfoGlobal, typeInfoPtr, singletonDeclarations)
    }

    private fun createSingletonDeclarations(
            descriptor: ClassDescriptor,
            typeInfoPtr: ConstPointer,
            bodyType: LLVMTypeRef
    ): SingletonLlvmDeclarations? {

        if (descriptor.isUnit()) {
            this.theUnitInstanceRef = staticData.createUnitInstance(descriptor, bodyType, typeInfoPtr)
            return null
        }

        val symbolName = if (descriptor.isExported()) {
            descriptor.objectInstanceFieldSymbolName
        } else {
            "kobjref:" + qualifyInternalName(descriptor)
        }
        val instanceFieldRef = LLVMAddGlobal(
                context.llvmModule,
                getLLVMType(descriptor.defaultType),
                symbolName
        )!!

        return SingletonLlvmDeclarations(instanceFieldRef)
    }

    override fun visitField(declaration: IrField) {
        super.visitField(declaration)

        val descriptor = declaration.descriptor

        val dispatchReceiverParameter = descriptor.dispatchReceiverParameter
        if (dispatchReceiverParameter != null) {
            val containingClass = dispatchReceiverParameter.containingDeclaration
            val classDeclarations = this.classes[containingClass] ?: error(containingClass.toString())

            val allFields = classDeclarations.fields

            this.fields[descriptor] = FieldLlvmDeclarations(
                    allFields.indexOf(descriptor),
                    classDeclarations.bodyType
            )
        } else {

            // Fields are module-private, so we use internal name:
            val name = "kvar:" + qualifyInternalName(descriptor)

            val storage = LLVMAddGlobal(context.llvmModule, getLLVMType(descriptor.type), name)!!

            this.staticFields[descriptor] = StaticFieldLlvmDeclarations(storage)
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        super.visitFunction(declaration)

        val descriptor = declaration.descriptor
        val llvmFunctionType = getLlvmFunctionType(descriptor)

        val llvmFunction = if (descriptor.isExternal) {
            context.llvm.externalFunction(descriptor.symbolName, llvmFunctionType)
        } else {
            val symbolName = if (descriptor.isExported()) {
                descriptor.symbolName
            } else {
                "kfun:" + qualifyInternalName(descriptor)
            }
            LLVMAddFunction(context.llvmModule, symbolName, llvmFunctionType)!!
        }

        this.functions[descriptor] = FunctionLlvmDeclarations(llvmFunction)
    }
}

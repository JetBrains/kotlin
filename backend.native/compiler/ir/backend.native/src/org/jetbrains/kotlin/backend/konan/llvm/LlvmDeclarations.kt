/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.irasdescriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal fun createLlvmDeclarations(context: Context): LlvmDeclarations {
    val generator = DeclarationsGeneratorVisitor(context)
    context.ir.irModule.acceptChildrenVoid(generator)
    return with(generator) {
        LlvmDeclarations(
                functions, classes, fields, staticFields, uniques
        )
    }
}

// Please note, that llvmName is part of the ABI, and cannot be liberally changed.
enum class UniqueKind(val llvmName: String) {
    UNIT("theUnitInstance"),
    EMPTY_ARRAY("theEmptyArray")
}

internal class LlvmDeclarations(
        private val functions: Map<FunctionDescriptor, FunctionLlvmDeclarations>,
        private val classes: Map<ClassDescriptor, ClassLlvmDeclarations>,
        private val fields: Map<IrField, FieldLlvmDeclarations>,
        private val staticFields: Map<IrField, StaticFieldLlvmDeclarations>,
        private val unique: Map<UniqueKind, UniqueLlvmDeclarations>) {
    fun forFunction(descriptor: FunctionDescriptor) = functions[descriptor] ?:
            error(descriptor.toString())

    fun forClass(descriptor: ClassDescriptor) = classes[descriptor] ?:
            error(descriptor.toString())

    fun forField(descriptor: IrField) = fields[descriptor] ?:
            error(descriptor.toString())

    fun forStaticField(descriptor: IrField) = staticFields[descriptor] ?:
            error(descriptor.toString())

    fun forSingleton(descriptor: ClassDescriptor) = forClass(descriptor).singletonDeclarations ?:
            error(descriptor.toString())

    fun forUnique(kind: UniqueKind) = unique[kind] ?: error("No unique $kind")

}

internal class ClassLlvmDeclarations(
        val bodyType: LLVMTypeRef,
        val fields: List<IrField>, // TODO: it is not an LLVM declaration.
        val typeInfoGlobal: StaticData.Global,
        val writableTypeInfoGlobal: StaticData.Global?,
        val typeInfo: ConstPointer,
        val singletonDeclarations: SingletonLlvmDeclarations?,
        val objCDeclarations: KotlinObjCClassLlvmDeclarations?)

internal class SingletonLlvmDeclarations(val instanceFieldRef: LLVMValueRef, val instanceShadowFieldRef: LLVMValueRef?)

internal class KotlinObjCClassLlvmDeclarations(
        val classPointerGlobal: StaticData.Global,
        val classInfoGlobal: StaticData.Global,
        val bodyOffsetGlobal: StaticData.Global
)

internal class FunctionLlvmDeclarations(val llvmFunction: LLVMValueRef)

internal class FieldLlvmDeclarations(val index: Int, val classBodyType: LLVMTypeRef)

internal class StaticFieldLlvmDeclarations(val storage: LLVMValueRef)

internal class UniqueLlvmDeclarations(val pointer: ConstPointer)

// TODO: rework getFields and getDeclaredFields.

/**
 * All fields of the class instance.
 * The order respects the class hierarchy, i.e. a class [fields] contains superclass [fields] as a prefix.
 */
internal fun ContextUtils.getFields(classDescriptor: ClassDescriptor) = context.getFields(classDescriptor)

internal fun Context.getFields(classDescriptor: ClassDescriptor): List<IrField> {
    val superClass = classDescriptor.getSuperClassNotAny() // TODO: what if Any has fields?
    val superFields = if (superClass != null) getFields(superClass) else emptyList()

    return superFields + getDeclaredFields(classDescriptor)
}

/**
 * Fields declared in the class.
 */
private fun Context.getDeclaredFields(classDescriptor: ClassDescriptor): List<IrField> {
    // TODO: Here's what is going on here:
    // The existence of a backing field for a property is only described in the IR,
    // but not in the PropertyDescriptor.
    //
    // We mark serialized properties with a Konan protobuf extension bit,
    // so it is present in DeserializedPropertyDescriptor.
    //
    // In this function we check the presence of the backing field
    // two ways: first we check IR, then we check the protobuf extension.

    val irClass = classDescriptor
    val fields = irClass.declarations.mapNotNull {
        when (it) {
            is IrField -> it.takeIf { it.isReal }
            is IrProperty -> it.takeIf { it.isReal }?.konanBackingField
            else -> null
        }
    }

    return fields.sortedBy {
        it.fqNameSafe.localHash.value
    }
}

private fun ContextUtils.createClassBodyType(name: String, fields: List<IrField>): LLVMTypeRef {
    val fieldTypes = fields.map {
        @Suppress("DEPRECATION")
        getLLVMType(if (it.isDelegate) context.irBuiltIns.anyNType else it.type)
    }

    val classType = LLVMStructCreateNamed(LLVMGetModuleContext(context.llvmModule), name)!!

    LLVMStructSetBody(classType, fieldTypes.toCValues(), fieldTypes.size, 0)

    return classType
}

private class DeclarationsGeneratorVisitor(override val context: Context) :
        IrElementVisitorVoid, ContextUtils {

    val functions = mutableMapOf<FunctionDescriptor, FunctionLlvmDeclarations>()
    val classes = mutableMapOf<ClassDescriptor, ClassLlvmDeclarations>()
    val fields = mutableMapOf<IrField, FieldLlvmDeclarations>()
    val staticFields = mutableMapOf<IrField, StaticFieldLlvmDeclarations>()
    val uniques = mutableMapOf<UniqueKind, UniqueLlvmDeclarations>()

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
        if (descriptor.isAnonymousObject) {
            return objectNamer.getName(parent, descriptor)
        }

        return descriptor.name
    }

    private fun getFqName(descriptor: DeclarationDescriptor): FqName {
        val parent = descriptor.parent
        val parentFqName = when (parent) {
            is IrPackageFragment -> parent.fqName
            is IrDeclaration -> getFqName(parent)
            else -> error(parent)
        }

        val localName = getLocalName(parentFqName, descriptor)
        return parentFqName.child(localName)
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

        if (declaration.isIntrinsic) {
            // do not generate any declarations for intrinsic classes as they require special handling
        } else {
            this.classes[declaration] = createClassDeclarations(declaration)
        }

        super.visitClass(declaration)
    }

    private fun createClassDeclarations(declaration: IrClass): ClassLlvmDeclarations {
        val descriptor = declaration

        val internalName = qualifyInternalName(descriptor)

        val fields = getFields(descriptor)
        val bodyType = createClassBodyType("kclassbody:$internalName", fields)

        val typeInfoPtr: ConstPointer
        val typeInfoGlobal: StaticData.Global

        val typeInfoSymbolName = if (descriptor.isExported()) {
            descriptor.typeInfoSymbolName
        } else {
            "ktype:$internalName"
        }

        if (descriptor.typeInfoHasVtableAttached) {
            // Create the special global consisting of TypeInfo and vtable.

            val typeInfoGlobalName = "ktypeglobal:$internalName"

            val typeInfoWithVtableType = structType(
                    runtime.typeInfoType,
                    LLVMArrayType(int8TypePtr, context.getVtableBuilder(descriptor).vtableEntries.size)!!
            )

            typeInfoGlobal = staticData.createGlobal(typeInfoWithVtableType, typeInfoGlobalName, isExported = false)

            val llvmTypeInfoPtr = LLVMAddAlias(context.llvmModule,
                    kTypeInfoPtr,
                    typeInfoGlobal.pointer.getElementPtr(0).llvm,
                    typeInfoSymbolName)!!

            if (descriptor.isExported()) {
                if (llvmTypeInfoPtr.name != typeInfoSymbolName) {
                    // So alias name has been mangled by LLVM to avoid name clash.
                    throw IllegalArgumentException("Global '$typeInfoSymbolName' already exists")
                }
            } else {
                LLVMSetLinkage(llvmTypeInfoPtr, LLVMLinkage.LLVMInternalLinkage)
            }

            typeInfoPtr = constPointer(llvmTypeInfoPtr)

        } else {
            typeInfoGlobal = staticData.createGlobal(runtime.typeInfoType,
                    typeInfoSymbolName,
                    isExported = descriptor.isExported())

            typeInfoPtr = typeInfoGlobal.pointer
        }

        if (descriptor.isUnit() || descriptor.isKotlinArray())
            createUniqueDeclarations(descriptor, typeInfoPtr, bodyType)

        val singletonDeclarations = if (descriptor.kind.isSingleton) {
            createSingletonDeclarations(descriptor)
        } else {
            null
        }

        val objCDeclarations = if (descriptor.isKotlinObjCClass()) {
            createKotlinObjCClassDeclarations(descriptor)
        } else {
            null
        }

        val writableTypeInfoType = runtime.writableTypeInfoType
        val writableTypeInfoGlobal = if (writableTypeInfoType == null) {
            null
        } else if (descriptor.isExported()) {
            val name = descriptor.writableTypeInfoSymbolName
            staticData.createGlobal(writableTypeInfoType, name, isExported = true).also {
                it.setLinkage(LLVMLinkage.LLVMCommonLinkage) // Allows to be replaced by other bitcode module.
            }
        } else {
            staticData.createGlobal(writableTypeInfoType, "")
        }.also {
            it.setZeroInitializer()
        }

        return ClassLlvmDeclarations(bodyType, fields, typeInfoGlobal, writableTypeInfoGlobal, typeInfoPtr,
                singletonDeclarations, objCDeclarations)
    }

    private fun createUniqueDeclarations(
            descriptor: ClassDescriptor, typeInfoPtr: ConstPointer, bodyType: LLVMTypeRef) {
        when {
                descriptor.isUnit() -> {
                    uniques[UniqueKind.UNIT] =
                            UniqueLlvmDeclarations(staticData.createUniqueInstance(UniqueKind.UNIT, bodyType, typeInfoPtr))
                }
                descriptor.isKotlinArray() -> {
                    uniques[UniqueKind.EMPTY_ARRAY] =
                            UniqueLlvmDeclarations(staticData.createUniqueInstance(UniqueKind.EMPTY_ARRAY, bodyType, typeInfoPtr))
                }
                else -> TODO("Unsupported unique $descriptor")
        }
    }

    private fun createSingletonDeclarations(descriptor: ClassDescriptor): SingletonLlvmDeclarations? {

        if (descriptor.isUnit()) {
            return null
        }

        val isExported = descriptor.isExported()
        val symbolName = if (isExported) {
            descriptor.objectInstanceFieldSymbolName
        } else {
            "kobjref:" + qualifyInternalName(descriptor)
        }
        val threadLocal = !(descriptor.symbol.objectIsShared && context.config.threadsAreAllowed)
        val instanceFieldRef = addGlobal(
                symbolName, getLLVMType(descriptor.defaultType), isExported = isExported, threadLocal = threadLocal)

        LLVMSetInitializer(instanceFieldRef, kNullObjHeaderPtr)

        val instanceShadowFieldRef =
                if (threadLocal) null
                else {
                    val shadowSymbolName = if (isExported) {
                        descriptor.objectInstanceShadowFieldSymbolName
                    } else {
                        "kshadowobjref:" + qualifyInternalName(descriptor)
                    }
                    addGlobal(shadowSymbolName, getLLVMType(descriptor.defaultType), isExported = isExported, threadLocal = true)
                }

        instanceShadowFieldRef?.let { LLVMSetInitializer(it, kNullObjHeaderPtr) }

        return SingletonLlvmDeclarations(instanceFieldRef, instanceShadowFieldRef)
    }

    private fun createKotlinObjCClassDeclarations(descriptor: ClassDescriptor): KotlinObjCClassLlvmDeclarations {
        val internalName = qualifyInternalName(descriptor)

        val classPointerGlobal = staticData.createGlobal(int8TypePtr, "kobjcclassptr:$internalName")

        val classInfoGlobal = staticData.createGlobal(
                context.llvm.runtime.kotlinObjCClassInfo,
                "kobjcclassinfo:$internalName"
        ).apply {
            setConstant(true)
        }

        val bodyOffsetGlobal = staticData.createGlobal(int32Type, "kobjcbodyoffs:$internalName")

        return KotlinObjCClassLlvmDeclarations(classPointerGlobal, classInfoGlobal, bodyOffsetGlobal)
    }

    override fun visitField(declaration: IrField) {
        super.visitField(declaration)

        val descriptor = declaration

        val containingClass = descriptor.containingClass
        if (containingClass != null) {
            val classDeclarations = this.classes[containingClass] ?: error(containingClass.toString())

            val allFields = classDeclarations.fields

            this.fields[descriptor] = FieldLlvmDeclarations(
                    allFields.indexOf(descriptor),
                    classDeclarations.bodyType
            )
        } else {

            // Fields are module-private, so we use internal name:
            val name = "kvar:" + qualifyInternalName(descriptor)

            val storage = addGlobal(
                    name, getLLVMType(descriptor.type), isExported = false, threadLocal = true)

            this.staticFields[descriptor] = StaticFieldLlvmDeclarations(storage)
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        super.visitFunction(declaration)

        if (!declaration.isReal) return

        val descriptor = declaration
        val llvmFunctionType = getLlvmFunctionType(descriptor)

        if ((descriptor is ConstructorDescriptor && descriptor.isObjCConstructor())) {
            return
        }

        val llvmFunction = if (descriptor.isExternal) {
            if (descriptor.isIntrinsic || descriptor.isObjCBridgeBased()) {
                return
            }

            context.llvm.externalFunction(descriptor.symbolName, llvmFunctionType,
                    // Assume that `external fun` is defined in native libs attached to this module:
                    origin = descriptor.llvmSymbolOrigin
            )
        } else {
            val symbolName = if (descriptor.isExported()) {
                descriptor.symbolName.also {
                    if (!descriptor.isMain()) {
                        assert(LLVMGetNamedFunction(context.llvm.llvmModule, it) == null) { it }
                    } else {
                        // As a workaround, allow `main` functions to clash because frontend accepts this.
                        // See [OverloadResolver.isTopLevelMainInDifferentFiles] usage.
                    }
                }
            } else {
                "kfun:" + qualifyInternalName(descriptor)
            }
            LLVMAddFunction(context.llvmModule, symbolName, llvmFunctionType)!!
        }

        // TODO: do we still need it?
        if (!context.shouldOptimize()) {
            LLVMAddTargetDependentFunctionAttr(llvmFunction, "no-frame-pointer-elim", "true")
        }

        this.functions[descriptor] = FunctionLlvmDeclarations(llvmFunction)
    }
}

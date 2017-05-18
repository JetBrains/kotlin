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

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMDumpModule
import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.jvm.descriptors.initialize
import org.jetbrains.kotlin.backend.common.validateIrModule
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.DumpIrTreeWithDescriptorsVisitor
import org.jetbrains.kotlin.backend.konan.ir.Ir
import org.jetbrains.kotlin.backend.konan.library.LinkData
import org.jetbrains.kotlin.backend.konan.library.KonanLibraryWriter
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addIfNotNull
import java.lang.System.out
import java.util.*
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KProperty

internal class SpecialDeclarationsFactory(val context: Context) {
    private val enumSpecialDeclarationsFactory by lazy { EnumSpecialDeclarationsFactory(context) }
    private val outerThisFields = mutableMapOf<ClassDescriptor, IrField>()
    private val bridgesDescriptors = mutableMapOf<Pair<FunctionDescriptor, BridgeDirections>, FunctionDescriptor>()
    private val loweredEnums = mutableMapOf<ClassDescriptor, LoweredEnum>()

    object DECLARATION_ORIGIN_FIELD_FOR_OUTER_THIS :
            IrDeclarationOriginImpl("FIELD_FOR_OUTER_THIS")

    fun getOuterThisField(innerClassDescriptor: ClassDescriptor): IrField =
        if (!innerClassDescriptor.isInner) throw AssertionError("Class is not inner: $innerClassDescriptor")
        else outerThisFields.getOrPut(innerClassDescriptor) {
            val outerClassDescriptor = DescriptorUtils.getContainingClass(innerClassDescriptor) ?:
                    throw AssertionError("No containing class for inner class $innerClassDescriptor")

            val receiver = ReceiverParameterDescriptorImpl(innerClassDescriptor, ImplicitClassReceiver(innerClassDescriptor))
            val descriptor = PropertyDescriptorImpl.create(
                    innerClassDescriptor, Annotations.EMPTY, Modality.FINAL,
                    Visibilities.PRIVATE, false, "this$0".synthesizedName, CallableMemberDescriptor.Kind.SYNTHESIZED,
                    SourceElement.NO_SOURCE, false, false, false, false, false, false
            ).initialize(outerClassDescriptor.defaultType, dispatchReceiverParameter = receiver)

            IrFieldImpl(
                    innerClassDescriptor.startOffsetOrUndefined,
                    innerClassDescriptor.endOffsetOrUndefined,
                    DECLARATION_ORIGIN_FIELD_FOR_OUTER_THIS,
                    descriptor
            )
        }

    fun getBridgeDescriptor(overriddenFunctionDescriptor: OverriddenFunctionDescriptor): FunctionDescriptor {
        val descriptor = overriddenFunctionDescriptor.descriptor.original
        assert(overriddenFunctionDescriptor.needBridge,
                { "Function $descriptor is not needed in a bridge to call overridden function ${overriddenFunctionDescriptor.overriddenDescriptor}" })
        val bridgeDirections = overriddenFunctionDescriptor.bridgeDirections
        return bridgesDescriptors.getOrPut(descriptor to bridgeDirections) {
            SimpleFunctionDescriptorImpl.create(
                    /* containingDeclaration = */ descriptor.containingDeclaration,
                    /* annotations           = */ Annotations.EMPTY,
                    /* name                  = */ "<bridge-$bridgeDirections>${descriptor.functionName}".synthesizedName,
                    /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                    /* source                = */ SourceElement.NO_SOURCE).apply {
                initializeBridgeDescriptor(this, descriptor, bridgeDirections.array)
            }
        }
    }

    fun getLoweredEnum(enumClassDescriptor: ClassDescriptor): LoweredEnum {
        assert(enumClassDescriptor.kind == ClassKind.ENUM_CLASS, { "Expected enum class but was: $enumClassDescriptor" })
        return loweredEnums.getOrPut(enumClassDescriptor) {
            enumSpecialDeclarationsFactory.createLoweredEnum(enumClassDescriptor)
        }
    }

    private fun initializeBridgeDescriptor(bridgeDescriptor: SimpleFunctionDescriptorImpl,
                                           descriptor: FunctionDescriptor,
                                           bridgeDirections: Array<BridgeDirection>) {
        val returnType = when (bridgeDirections[0]) {
            BridgeDirection.TO_VALUE_TYPE   -> descriptor.returnType!!
            BridgeDirection.NOT_NEEDED      -> descriptor.returnType
            BridgeDirection.FROM_VALUE_TYPE -> context.builtIns.anyType
        }

        val extensionReceiverType = when (bridgeDirections[1]) {
            BridgeDirection.TO_VALUE_TYPE   -> descriptor.extensionReceiverParameter!!.type
            BridgeDirection.NOT_NEEDED      -> descriptor.extensionReceiverParameter?.type
            BridgeDirection.FROM_VALUE_TYPE -> context.builtIns.anyType
        }

        val valueParameters = descriptor.valueParameters.mapIndexed { index, valueParameterDescriptor ->
                val outType = when (bridgeDirections[index + 2]) {
                    BridgeDirection.TO_VALUE_TYPE   -> valueParameterDescriptor.type
                    BridgeDirection.NOT_NEEDED      -> valueParameterDescriptor.type
                    BridgeDirection.FROM_VALUE_TYPE -> context.builtIns.anyType
                }
                ValueParameterDescriptorImpl(
                    containingDeclaration = valueParameterDescriptor.containingDeclaration,
                    original              = null,
                    index                 = index,
                    annotations           = Annotations.EMPTY,
                    name                  = valueParameterDescriptor.name,
                    outType               = outType,
                    declaresDefaultValue  = valueParameterDescriptor.declaresDefaultValue(),
                    isCrossinline         = valueParameterDescriptor.isCrossinline,
                    isNoinline            = valueParameterDescriptor.isNoinline,
                    varargElementType     = valueParameterDescriptor.varargElementType,
                    source                = SourceElement.NO_SOURCE)
        }
        bridgeDescriptor.initialize(
                /* receiverParameterType        = */ extensionReceiverType,
                /* dispatchReceiverParameter    = */ descriptor.dispatchReceiverParameter,
                /* typeParameters               = */ descriptor.typeParameters,
                /* unsubstitutedValueParameters = */ valueParameters,
                /* unsubstitutedReturnType      = */ returnType,
                /* modality                     = */ descriptor.modality,
                /* visibility                   = */ descriptor.visibility).apply {
            isSuspend                           =    descriptor.isSuspend
        }
    }
}


class ReflectionTypes(module: ModuleDescriptor) {
    val KOTLIN_REFLECT_FQ_NAME = FqName("kotlin.reflect")
    val KONAN_INTERNAL_FQ_NAME = FqName("konan.internal")

    private val kotlinReflectScope: MemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.getPackage(KOTLIN_REFLECT_FQ_NAME).memberScope
    }

    private val konanInternalScope: MemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.getPackage(KONAN_INTERNAL_FQ_NAME).memberScope
    }

    private fun find(memberScope: MemberScope, className: String): ClassDescriptor {
        val name = Name.identifier(className)
        return memberScope.getContributedClassifier(name, NoLookupLocation.FROM_REFLECTION) as ClassDescriptor
    }

    private class ClassLookup(val memberScope: MemberScope) {
        operator fun getValue(types: ReflectionTypes, property: KProperty<*>): ClassDescriptor {
            return types.find(memberScope, property.name.capitalize())
        }
    }

    private fun getFunctionTypeArgumentProjections(
            receiverType: KotlinType?,
            parameterTypes: List<KotlinType>,
            returnType: KotlinType
    ): List<TypeProjection> {
        val arguments = ArrayList<TypeProjection>(parameterTypes.size + (if (receiverType != null) 1 else 0) + 1)

        arguments.addIfNotNull(receiverType?.asTypeProjection())

        parameterTypes.mapTo(arguments, KotlinType::asTypeProjection)

        arguments.add(returnType.asTypeProjection())

        return arguments
    }

    fun getKFunction(n: Int): ClassDescriptor = find(kotlinReflectScope, "KFunction$n")

    val kClass: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kProperty0: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kProperty1: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kProperty2: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kMutableProperty0: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kMutableProperty1: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kMutableProperty2: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kProperty0Impl: ClassDescriptor by ClassLookup(konanInternalScope)
    val kProperty1Impl: ClassDescriptor by ClassLookup(konanInternalScope)
    val kProperty2Impl: ClassDescriptor by ClassLookup(konanInternalScope)
    val kMutableProperty0Impl: ClassDescriptor by ClassLookup(konanInternalScope)
    val kMutableProperty1Impl: ClassDescriptor by ClassLookup(konanInternalScope)
    val kMutableProperty2Impl: ClassDescriptor by ClassLookup(konanInternalScope)
    val kLocalDelegatedPropertyImpl: ClassDescriptor by ClassLookup(konanInternalScope)
    val kLocalDelegatedMutablePropertyImpl: ClassDescriptor by ClassLookup(konanInternalScope)

    fun getKFunctionType(
            annotations: Annotations,
            receiverType: KotlinType?,
            parameterTypes: List<KotlinType>,
            returnType: KotlinType
    ): KotlinType {
        val arguments = getFunctionTypeArgumentProjections(receiverType, parameterTypes, returnType)
        val classDescriptor = getKFunction(arguments.size - 1 /* return type */)
        return KotlinTypeFactory.simpleNotNullType(annotations, classDescriptor, arguments)
    }
}

internal class Context(config: KonanConfig) : KonanBackendContext(config) {
    lateinit var moduleDescriptor: ModuleDescriptor

    override val builtIns: KonanBuiltIns by lazy(PUBLICATION) { moduleDescriptor.builtIns as KonanBuiltIns }

    val specialDeclarationsFactory = SpecialDeclarationsFactory(this)
    val reflectionTypes: ReflectionTypes by lazy(PUBLICATION) { ReflectionTypes(moduleDescriptor) }
    private val vtableBuilders = mutableMapOf<ClassDescriptor, ClassVtablesBuilder>()

    fun getVtableBuilder(classDescriptor: ClassDescriptor) = vtableBuilders.getOrPut(classDescriptor) {
        ClassVtablesBuilder(classDescriptor, this)
    }

    // We serialize untouched descriptor tree and inline IR bodies
    // right after the frontend.
    // But we have to wait until the code generation phase,
    // to dump this information into generated file.
    var serializedLinkData: LinkData? = null

    @Deprecated("")
    lateinit var psi2IrGeneratorContext: GeneratorContext

    // TODO: make lateinit?
    var irModule: IrModuleFragment? = null
        set(module: IrModuleFragment?) {
            if (field != null) {
                throw Error("Another IrModule in the context.")
            }
            field = module!!

            ir = Ir(this, module)
        }

    override lateinit var ir: Ir

    override val irBuiltIns
        get() = ir.irModule.irBuiltins

    val interopBuiltIns by lazy {
        InteropBuiltIns(this.builtIns)
    }

    var llvmModule: LLVMModuleRef? = null
        set(module: LLVMModuleRef?) {
            if (field != null) {
                throw Error("Another LLVMModule in the context.")
            }
            field = module!!

            llvm = Llvm(this, module)
            debugInfo = DebugInfo(this)
        }

    lateinit var llvm: Llvm
    lateinit var llvmDeclarations: LlvmDeclarations
    lateinit var bitcodeFileName: String
    lateinit var library: KonanLibraryWriter

    var phase: KonanPhase? = null
    var depth: Int = 0

    protected fun separator(title: String) {
        println("\n\n--- ${title} ----------------------\n")
    }

    fun verifyDescriptors() {
        // TODO: Nothing here for now.
    }

    fun printDescriptors() {
        // A workaround to check if the lateinit field is assigned, see KT-9327
        try { moduleDescriptor } catch (e: UninitializedPropertyAccessException) { return }

        separator("Descriptors after: ${phase?.description}")
        moduleDescriptor.deepPrint()
    }

    fun verifyIr() {
        val module = irModule ?: return
        validateIrModule(this, module)
    }

    fun printIr() {
        if (irModule == null) return
        separator("IR after: ${phase?.description}")
        irModule!!.accept(DumpIrTreeVisitor(out), "")
    }

    fun printIrWithDescriptors() {
        if (irModule == null) return
        separator("IR after: ${phase?.description}")
        irModule!!.accept(DumpIrTreeWithDescriptorsVisitor(out), "")
    }

    fun printLocations() {
        if (irModule == null) return
        separator("Locations after: ${phase?.description}")
        irModule!!.acceptVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFile(declaration: IrFile) {
                val fileEntry = declaration.fileEntry
                declaration.acceptChildrenVoid(object:IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitFunction(declaration: IrFunction) {
                        super.visitFunction(declaration)
                        val descriptor = declaration.descriptor
                        println("${descriptor.fqNameOrNull()?: descriptor.name}: ${fileEntry.range(declaration)}")
                    }
                })
            }

            fun SourceManager.FileEntry.range(element:IrElement):String {
                try {
                    /* wasn't use multi line string to prevent appearing odd line
                     * breaks in the dump. */
                    return "${this.name}: ${this.line(element.startOffset)}" +
                          ":${this.column(element.startOffset)} - " +
                          "${this.line(element.endOffset)}" +
                          ":${this.column(element.endOffset)}"

                } catch (e:Exception) {
                    return "${this.name}: ERROR(${e.javaClass.name}): ${e.message}"
                }
            }
            fun SourceManager.FileEntry.line(offset:Int) = this.getLineNumber(offset)
            fun SourceManager.FileEntry.column(offset:Int) = this.getColumnNumber(offset)
        })
    }

    fun verifyBitCode() {
        if (llvmModule == null) return
        verifyModule(llvmModule!!)
    }

    fun printBitCode() {
        if (llvmModule == null) return
        separator("BitCode after: ${phase?.description}")
        LLVMDumpModule(llvmModule!!)
    }

    fun verify() {
        verifyDescriptors()
        verifyIr()
        verifyBitCode()
    }

    fun print() {
        printDescriptors()
        printIr()
        printBitCode()
    }

    fun shouldVerifyDescriptors(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.VERIFY_DESCRIPTORS) 
    }

    fun shouldVerifyIr(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.VERIFY_IR) 
    }

    fun shouldVerifyBitCode(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.VERIFY_BITCODE) 
    }

    fun shouldPrintDescriptors(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.PRINT_DESCRIPTORS) 
    }

    fun shouldPrintIr(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.PRINT_IR) 
    }

    fun shouldPrintIrWithDescriptors(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.PRINT_IR_WITH_DESCRIPTORS)
    }

    fun shouldPrintBitCode(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.PRINT_BITCODE) 
    }

    fun shouldPrintLocations(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.PRINT_LOCATIONS)
    }

    fun shouldProfilePhases(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.TIME_PHASES) 
    }

    fun shouldContainDebugInfo(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.DEBUG)
    }

    fun log(message: () -> String) {
        if (phase?.verbose ?: false) {
            println(message())
        }
    }

    lateinit var debugInfo:DebugInfo
}


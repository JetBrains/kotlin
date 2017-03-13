package org.jetbrains.kotlin.backend.konan

import llvm.LLVMDumpModule
import llvm.LLVMModuleRef
import org.jetbrains.kotlin.backend.common.validateIrModule
import org.jetbrains.kotlin.backend.jvm.descriptors.initialize
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.Ir
import org.jetbrains.kotlin.backend.konan.llvm.Llvm
import org.jetbrains.kotlin.backend.konan.llvm.LlvmDeclarations
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.backend.konan.llvm.verifyModule
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.DumpIrTreeVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addIfNotNull
import java.lang.System.out
import java.util.*
import kotlin.reflect.KProperty

internal class SpecialDescriptorsFactory(val context: Context) {
    private val enumSpecialDescriptorsFactory by lazy { EnumSpecialDescriptorsFactory(context) }
    private val outerThisDescriptors = mutableMapOf<ClassDescriptor, PropertyDescriptor>()
    private val bridgesDescriptors = mutableMapOf<Pair<FunctionDescriptor, BridgeDirections>, FunctionDescriptor>()
    private val loweredEnums = mutableMapOf<ClassDescriptor, LoweredEnum>()

    fun getOuterThisFieldDescriptor(innerClassDescriptor: ClassDescriptor): PropertyDescriptor =
        if (!innerClassDescriptor.isInner) throw AssertionError("Class is not inner: $innerClassDescriptor")
        else outerThisDescriptors.getOrPut(innerClassDescriptor) {
            val outerClassDescriptor = DescriptorUtils.getContainingClass(innerClassDescriptor) ?:
                    throw AssertionError("No containing class for inner class $innerClassDescriptor")

            val receiver = ReceiverParameterDescriptorImpl(innerClassDescriptor, ImplicitClassReceiver(innerClassDescriptor))
            PropertyDescriptorImpl.create(innerClassDescriptor, Annotations.EMPTY, Modality.FINAL, Visibilities.PRIVATE,
                false, "this$0".synthesizedName, CallableMemberDescriptor.Kind.SYNTHESIZED, SourceElement.NO_SOURCE,
                false, false, false, false, false, false).initialize(outerClassDescriptor.defaultType, dispatchReceiverParameter = receiver)
        }

    fun getBridgeDescriptor(overriddenFunctionDescriptor: OverriddenFunctionDescriptor): FunctionDescriptor {
        val descriptor = overriddenFunctionDescriptor.descriptor.original
        assert(overriddenFunctionDescriptor.needBridge,
                { "Function $descriptor is not needed in a bridge to call overridden function ${overriddenFunctionDescriptor.overriddenDescriptor}" })
        val bridgeDirections = overriddenFunctionDescriptor.bridgeDirections
        return bridgesDescriptors.getOrPut(descriptor to bridgeDirections) {
            SimpleFunctionDescriptorImpl.create(
                    descriptor.containingDeclaration,
                    Annotations.EMPTY,
                    ("<bridge-" + bridgeDirections.toString() + ">" + descriptor.functionName).synthesizedName,
                    CallableMemberDescriptor.Kind.DECLARATION,
                    SourceElement.NO_SOURCE).apply {
                initializeBridgeDescriptor(this, descriptor, bridgeDirections.array)
            }
        }
    }

    fun getLoweredEnum(enumClassDescriptor: ClassDescriptor): LoweredEnum {
        assert(enumClassDescriptor.kind == ClassKind.ENUM_CLASS, { "Expected enum class but was: $enumClassDescriptor" })
        return loweredEnums.getOrPut(enumClassDescriptor) {
            enumSpecialDescriptorsFactory.createLoweredEnum(enumClassDescriptor)
        }
    }

    private fun initializeBridgeDescriptor(bridgeDescriptor: SimpleFunctionDescriptorImpl,
                                           descriptor: FunctionDescriptor,
                                           bridgeDirections: Array<BridgeDirection>) {
        val returnType = when (bridgeDirections[0]) {
            BridgeDirection.TO_VALUE_TYPE -> descriptor.returnType!!
            BridgeDirection.NOT_NEEDED -> descriptor.returnType
            BridgeDirection.FROM_VALUE_TYPE -> context.builtIns.anyType
        }

        val extensionReceiverType = when (bridgeDirections[1]) {
            BridgeDirection.TO_VALUE_TYPE -> descriptor.extensionReceiverParameter!!.type
            BridgeDirection.NOT_NEEDED -> descriptor.extensionReceiverParameter?.type
            BridgeDirection.FROM_VALUE_TYPE -> context.builtIns.anyType
        }

        bridgeDescriptor.initialize(
                extensionReceiverType,
                descriptor.dispatchReceiverParameter,
                descriptor.typeParameters,
                descriptor.valueParameters.mapIndexed { index, valueParameterDescriptor ->
                    when (bridgeDirections[index + 2]) {
                        BridgeDirection.TO_VALUE_TYPE -> valueParameterDescriptor
                        BridgeDirection.NOT_NEEDED -> valueParameterDescriptor
                        BridgeDirection.FROM_VALUE_TYPE -> ValueParameterDescriptorImpl(
                                valueParameterDescriptor.containingDeclaration,
                                null,
                                index,
                                Annotations.EMPTY,
                                valueParameterDescriptor.name,
                                context.builtIns.anyType,
                                valueParameterDescriptor.declaresDefaultValue(),
                                valueParameterDescriptor.isCrossinline,
                                valueParameterDescriptor.isNoinline,
                                valueParameterDescriptor.varargElementType,
                                SourceElement.NO_SOURCE)
                    }
                },
                returnType,
                descriptor.modality,
                descriptor.visibility)
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
    val kProperty0Impl: ClassDescriptor by ClassLookup(konanInternalScope)
    val kProperty1Impl: ClassDescriptor by ClassLookup(konanInternalScope)
    val kMutableProperty0Impl: ClassDescriptor by ClassLookup(konanInternalScope)
    val kMutableProperty1Impl: ClassDescriptor by ClassLookup(konanInternalScope)
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

    var moduleDescriptor: ModuleDescriptor? = null

    val specialDescriptorsFactory = SpecialDescriptorsFactory(this)
    val reflectionTypes: ReflectionTypes by lazy { ReflectionTypes(moduleDescriptor!!) }
    private val vtableBuilders = mutableMapOf<ClassDescriptor, ClassVtablesBuilder>()

    fun getVtableBuilder(classDescriptor: ClassDescriptor) = vtableBuilders.getOrPut(classDescriptor) {
        ClassVtablesBuilder(classDescriptor, this)
    }

    // TODO: make lateinit?
    var irModule: IrModuleFragment? = null
        set(module: IrModuleFragment?) {
            if (field != null) {
                throw Error("Another IrModule in the context.")
            }
            field = module!!

            ir = Ir(this, module)
        }

    lateinit var ir: Ir

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
        }

    lateinit var llvm: Llvm
    lateinit var llvmDeclarations: LlvmDeclarations

    var phase: KonanPhase? = null
    var depth: Int = 0

    protected fun separator(title: String) {
        println("\n\n--- ${title} ----------------------\n")
    }

    fun verifyDescriptors() {
        // TODO: Nothing here for now.
    }

    fun printDescriptors() {
        if (moduleDescriptor == null) return
        separator("Descriptors after: ${phase?.description}")
        moduleDescriptor!!.deepPrint()
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

    fun shouldPrintBitCode(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.PRINT_BITCODE) 
    }

    fun shouldProfilePhases(): Boolean {
        return config.configuration.getBoolean(KonanConfigKeys.TIME_PHASES) 
    }

    fun log(message: String) {
        if (phase?.verbose ?: false) {
            println(message)
        }
    }
}


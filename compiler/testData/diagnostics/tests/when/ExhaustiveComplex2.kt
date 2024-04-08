// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

// MODULE: declarations
// FILE declarations.kt

sealed class TypeBridge

object ReferenceBridge : TypeBridge()

data class BlockPointerBridge(
    val numberOfParameters: Int,
    val returnsVoid: Boolean,
) : TypeBridge()

data class ValueTypeBridge(val objCValueType: String) : TypeBridge()

sealed class MethodBridgeParameter

sealed class MethodBridgeReceiver : MethodBridgeParameter() {
    object Static : MethodBridgeReceiver()
    object Factory : MethodBridgeReceiver()
    object Instance : MethodBridgeReceiver()
}

object MethodBridgeSelector : MethodBridgeParameter()

sealed class MethodBridgeValueParameter : MethodBridgeParameter() {
    data class Mapped(val bridge: TypeBridge) : MethodBridgeValueParameter()
    object ErrorOutParameter : MethodBridgeValueParameter()
    data class SuspendCompletion(val useUnitCompletion: Boolean) : MethodBridgeValueParameter()
}

data class MethodBridge(
    val returnBridge: ReturnValue,
    val receiver: MethodBridgeReceiver,
    val valueParameters: List<MethodBridgeValueParameter>,
) {

    sealed class ReturnValue {
        object Void : ReturnValue()
        object HashCode : ReturnValue()
        data class Mapped(val bridge: TypeBridge) : ReturnValue()
        sealed class Instance : ReturnValue() {
            object InitResult : Instance()
            object FactoryResult : Instance()
        }

        sealed class WithError : ReturnValue() {
            object Success : WithError()
            data class ZeroForError(val successBridge: ReturnValue, val successMayBeZero: Boolean) : WithError()
        }

        object Suspend : ReturnValue()
    }
}

interface JavaModuleResolver {
    fun checkAccessibility(): AccessError?

    sealed class AccessError {
        object ModuleDoesNotReadUnnamedModule : AccessError()
        data class ModuleDoesNotReadModule(val dependencyModuleName: String) : AccessError()
        data class ModuleDoesNotExportPackage(val dependencyModuleName: String) : AccessError()
    }
}

// MODULE usage
// FILE usage.kt

fun mapType(typeBridge: TypeBridge): String = when (typeBridge) {
    ReferenceBridge -> "reference"
    is BlockPointerBridge -> "function"
    is ValueTypeBridge -> "value"
}

private fun mapReturnType(
    returnBridge: MethodBridge.ReturnValue,
): String = when (returnBridge) {
    MethodBridge.ReturnValue.Suspend,
    MethodBridge.ReturnValue.Void, -> "void"
    MethodBridge.ReturnValue.HashCode -> "integer"
    is MethodBridge.ReturnValue.Mapped -> "mapped"
    MethodBridge.ReturnValue.WithError.Success -> "bool"
    is MethodBridge.ReturnValue.WithError.ZeroForError -> mapReturnType(returnBridge.successBridge)
    MethodBridge.ReturnValue.Instance.InitResult,
    MethodBridge.ReturnValue.Instance.FactoryResult, -> "instance"
}

fun check(javaModuleResolver: JavaModuleResolver) {
    val diagnostic = javaModuleResolver.checkAccessibility() ?: return
    when (diagnostic) {
        is JavaModuleResolver.AccessError.ModuleDoesNotExportPackage -> { }
        is JavaModuleResolver.AccessError.ModuleDoesNotReadModule -> { }
        JavaModuleResolver.AccessError.ModuleDoesNotReadUnnamedModule -> { }
    }
}
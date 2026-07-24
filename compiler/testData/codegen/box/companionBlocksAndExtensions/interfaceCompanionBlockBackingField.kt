// LANGUAGE: +CompanionBlocks +CompanionExtensions
// IGNORE_BACKEND: JVM_IR
// JVM_IR KT-85853

interface InterfaceWithBackingField {
    companion {
        val interfaceVal = "InterfaceVal"
    }
}

companion fun InterfaceWithBackingField.extFun() = InterfaceWithBackingField.interfaceVal + "ExtFun"

class ImplWithBackingField : InterfaceWithBackingField {
    fun useInterfaceCompanion() = InterfaceWithBackingField.interfaceVal
}

fun box(): String {
    if (InterfaceWithBackingField.interfaceVal != "InterfaceVal") {
        return "FAIL: interfaceVal=${InterfaceWithBackingField.interfaceVal}"
    }

    if (InterfaceWithBackingField.extFun() != "InterfaceValExtFun") {
        return "FAIL: extFun=${InterfaceWithBackingField.extFun()}"
    }

    val impl = ImplWithBackingField()
    if (impl.useInterfaceCompanion() != "InterfaceVal") {
        return "FAIL: useInterfaceCompanion=${impl.useInterfaceCompanion()}"
    }

    return "OK"
}

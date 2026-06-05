// LANGUAGE: +CompanionBlocksAndExtensions
// IGNORE_BACKEND: NATIVE

interface MyInterface {
    companion {
        fun interfaceFun() = "InterfaceFun"
    }
}

companion fun MyInterface.extFun() = "ExtFun"

class Impl : MyInterface {
    fun useInterfaceCompanion() = MyInterface.interfaceFun()
}

fun box(): String {
    // Access companion block members on interface
    if (MyInterface.interfaceFun() != "InterfaceFun") return "FAIL: interfaceFun=${MyInterface.interfaceFun()}"

    // Access companion extensions on interface
    if (MyInterface.extFun() != "ExtFun") return "FAIL: extFun=${MyInterface.extFun()}"

    // Access from implementing class instance
    val impl = Impl()
    if (impl.useInterfaceCompanion() != "InterfaceFun") return "FAIL: useInterfaceCompanion=${impl.useInterfaceCompanion()}"

    return "OK"
}

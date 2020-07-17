open class Base() {
    open fun baseFun(): String = "Base.baseFun()"

    open fun unambiguous(): String = "Base.unambiguous()"

    open val baseProp: String
        get() = "Base.baseProp"
}

interface Interface {
    fun interfaceFun(): String = "Interface.interfaceFun()"

    fun unambiguous(): String // NB abstract
}

interface AnotherInterface

interface DerivedInterface: Interface, AnotherInterface {
    override fun interfaceFun(): String = "DerivedInterface.interfaceFun()"

    override fun unambiguous(): String = "DerivedInterface.unambiguous()"

    fun callsFunFromSuperInterface(): String = super.interfaceFun()
}

class Derived : Base(), Interface {
    override fun baseFun(): String = "Derived.baseFun()"

    override fun unambiguous(): String = "Derived.unambiguous()"

    override fun interfaceFun(): String = "Derived.interfaceFun()"

    override val baseProp: String
        get() = "Derived.baseProp"

    fun callsBaseFun(): String = super.baseFun()

    fun callsUnambiguousFun(): String = super.unambiguous()

    fun getsBaseProp(): String = super.baseProp

    fun callsInterfaceFun(): String = super.interfaceFun()
}

fun box(): String {
    val d = Derived()

    val test1 = d.callsBaseFun()
    if (test1 != "Base.baseFun()") return "Failed: d.callsBaseFun()==$test1"

    val test2 = d.callsUnambiguousFun()
    if (test2 != "Base.unambiguous()") return "Failed: d.callsUnambiguousFun()==$test2"

    val test3 = d.getsBaseProp()
    if (test3 != "Base.baseProp") return "Failed: d.getsBaseProp()==$test3"

    val test4 = d.callsInterfaceFun()
    if (test4 != "Interface.interfaceFun()") return "Failed: d.callsInterfaceFun()==$test4"

    val di = object : DerivedInterface {}

    val test5 = di.callsFunFromSuperInterface()
    if (test5 != "Interface.interfaceFun()") return "Failed: di.callsFunFromSuperInterface()==$test5"

    return "OK"
}

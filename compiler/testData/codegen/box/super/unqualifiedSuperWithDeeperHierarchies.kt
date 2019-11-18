// IGNORE_BACKEND_FIR: JVM_IR
open class DeeperBase {
    open fun deeperBaseFun(): String = "DeeperBase.deeperBaseFun()"

    open val deeperBaseProp: String
        get() = "DeeperBase.deeperBaseProp"
}

open class DeepBase : DeeperBase() {
}

interface DeeperInterface {
    fun deeperInterfaceFun(): String = "DeeperInterface.deeperInterfaceFun()"

    val deeperInterfaceProp: String
        get() = "DeeperInterface.deeperInterfaceProp"
}

interface DeepInterface : DeeperInterface {
    fun deepInterfaceFun(): String = "DeepInterface.deepInterfaceFun()"
}

class DeepDerived : DeepBase(), DeepInterface {
    override fun deeperBaseFun(): String = "DeepDerived.deeperBaseFun()"

    override val deeperBaseProp: String
        get() = "DeepDerived.deeperBaseProp"

    override fun deeperInterfaceFun(): String = "DeepDerived.deeperInterfaceFun()"

    override val deeperInterfaceProp: String
        get() = "DeepDerived.deeperInterfaceProp"

    override fun deepInterfaceFun(): String = "DeepDerived.deepInterfaceFun()"

    fun callsSuperDeeperBaseFun(): String = super.deeperBaseFun()

    fun getsSuperDeeperBaseProp(): String = super.deeperBaseProp

    fun callsSuperDeepInterfaceFun(): String = super.deepInterfaceFun()
    fun callsSuperDeeperInterfaceFun(): String = super.deeperInterfaceFun()
    fun getsSuperDeeperInterfaceProp(): String = super.deeperInterfaceProp
}

fun box(): String {
    val dd = DeepDerived()

    val test1 = dd.callsSuperDeeperBaseFun()
    if (test1 != "DeeperBase.deeperBaseFun()") return "Failed: dd.callsSuperDeeperBaseFun()==$test1"

    val test2 = dd.getsSuperDeeperBaseProp()
    if (test2 != "DeeperBase.deeperBaseProp") return "Failed: dd.getsSuperDeeperBaseProp()==$test2"

    val test3 = dd.callsSuperDeepInterfaceFun()
    if (test3 != "DeepInterface.deepInterfaceFun()") return "Failed: dd.callsSuperDeepInterfaceFun()==$test3"

    val test4 = dd.callsSuperDeeperInterfaceFun()
    if (test4 != "DeeperInterface.deeperInterfaceFun()") return "Failed: dd.callsSuperDeeperInterfaceFun()==$test4"

    val test5 = dd.getsSuperDeeperInterfaceProp()
    if (test5 != "DeeperInterface.deeperInterfaceProp") return "Failed: dd.getsSuperDeeperInterfaceProp()==$test5"

    return "OK"
}
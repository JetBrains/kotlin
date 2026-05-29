// LANGUAGE: +CompanionBlocksAndExtensions

abstract class Base {
    companion {
        val baseVal = "BaseVal"
        fun baseFun() = "BaseFun"
    }

    abstract fun abstractFun(): String
}

class Concrete : Base() {
    companion {
        val concreteVal = "ConcreteVal"
        fun concreteFun() = "ConcreteFun"
    }

    override fun abstractFun() = Base.baseFun() + concreteFun()
}

fun box(): String {
    if (Base.baseVal != "BaseVal") return "FAIL: Base.baseVal=${Base.baseVal}"
    if (Base.baseFun() != "BaseFun") return "FAIL: Base.baseFun=${Base.baseFun()}"

    if (Concrete.concreteVal != "ConcreteVal") return "FAIL: concreteVal=${Concrete.concreteVal}"
    if (Concrete.concreteFun() != "ConcreteFun") return "FAIL: concreteFun=${Concrete.concreteFun()}"

    val c = Concrete()
    if (c.abstractFun() != "BaseFunConcreteFun") return "FAIL: abstractFun=${c.abstractFun()}"

    return "OK"
}

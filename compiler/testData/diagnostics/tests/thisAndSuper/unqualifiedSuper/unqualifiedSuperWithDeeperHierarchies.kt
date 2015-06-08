// Check that it works with inherited members
//
//  DeeperBase  DeeperInterface
//      |          |
//  DeepBase    DeepInterface
//          \  /
//           \/
//       DeepDerived
//

open class DeeperBase {
    open fun deeperBaseFun() {}

    open val deeperBaseProp: Int
        get() = 333
}

open class DeepBase : DeeperBase() {
}

interface DeeperInterface {
    fun deeperInterfaceFun() {}
}

interface DeepInterface : DeeperInterface {
    fun deepInterfaceFun() {}
}

class DeepDerived : DeepBase(), DeepInterface {
    override fun deeperBaseFun() {}

    override val deeperBaseProp: Int
        get() = 444

    override fun deeperInterfaceFun() {}
    override fun deepInterfaceFun() {}

    fun callsSuperDeeperBaseFun() {
        super.deeperBaseFun()
    }

    fun getsSuperDeeperBaseProp(): Int =
            super.deeperBaseProp

    fun callsSuperInterfaceFuns() {
        super.deeperInterfaceFun()
        super.deepInterfaceFun()
        super<DeepInterface>.deeperInterfaceFun()
        super<DeepInterface>.deepInterfaceFun()
    }
}

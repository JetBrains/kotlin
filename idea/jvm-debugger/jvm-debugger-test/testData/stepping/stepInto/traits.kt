package traits

fun main(args: Array<String>) {
    //Breakpoint!
    val impl = MyInterfaceImpl()
    impl.inInterface()
    impl.propInInterface
    impl.inInterfaceOverride()
    impl.propInInterfaceOverride

    val o = object: MyInterface {
        override fun inInterfaceOverride(): Int {
            return 1
        }

        override val propInInterfaceOverride: Int
            get() = 1
    }
    o.inInterface()
    o.propInInterface
    o.inInterfaceOverride()
    o.propInInterfaceOverride
}

interface MyInterface {
    fun inInterface(): Int {
        return 1
    }

    val propInInterface: Int
        get() = 1

    fun inInterfaceOverride(): Int {
        return 1
    }

    val propInInterfaceOverride: Int
        get() = 1
}

class MyInterfaceImpl: MyInterface {
    override fun inInterfaceOverride(): Int {
        return 1
    }

    override val propInInterfaceOverride: Int
        get() = 1
}

// STEP_INTO: 38
// SKIP_CONSTRUCTORS: true
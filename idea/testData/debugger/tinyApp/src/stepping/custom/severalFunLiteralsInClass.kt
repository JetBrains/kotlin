package severalFunLiteralsInClass

fun main(args: Array<String>) {
    MyClass().inClass()
}

class MyClass {
    fun f1(f1Param: () -> Unit): MyClass {
        f1Param()
        return this
    }

    fun f2(f2Param: () -> Unit): MyClass {
        f2Param()
        return this
    }

    fun inClass() {
        //Breakpoint! (lambdaOrdinal = -1)
        f1 { test() }.f2 {
            test()
        }
    }
}

fun test() {}

// SMART_STEP_INTO_BY_INDEX: 4

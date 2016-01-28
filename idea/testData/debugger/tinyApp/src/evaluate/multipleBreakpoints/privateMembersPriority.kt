package privateMembersPriority

fun main(args: Array<String>) {
    Receiver2().test1()
    test2()
    test3()
}

class Receiver1 {
    private fun privateFun() = 1
}

class Receiver2 {
    fun privateFun() = 2

    // In IDE -> privateFun() will return 2, because privateFun from Receiver is invisible in current scope
    fun test1() {
        with(Receiver1()) {
            // EXPRESSION: privateFun()
            // RESULT: 1: I
            //Breakpoint!
            privateFun()
        }
    }
}

// In debuggerContext there are two properties size in ArrayList (java field + kotlin property)
fun test2() {
    val javaClass = arrayListOf(1)

    // EXPRESSION: javaClass.size
    // RESULT: 1: I
    //Breakpoint!
    javaClass.size
}

fun test3() {
    // EXPRESSION: TwoPrivateFun().foo(1)
    // RESULT: 1: I
    //Breakpoint!
    val a = 1
}

class TwoPrivateFun {
    private fun foo(i: Int) = 1
    private fun foo(i: Double) = 2
}
package inlineFunctionSameLines

fun main(args: Array<String>) {
    if (1 > 2) {
        val a = 1
    }
    else {
        val b = 2
    }

    //Breakpoint!
    myFun(1)
    val a = 1
}
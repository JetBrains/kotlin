package ceObject

fun main(args: Array<String>) {
    //Breakpoint!
    args.size()
}

trait T {
    fun test() = 1
}

//- EXPRESSION: (object: T {}).test()
//- RESULT: 1: I
package ceObject

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

interface T {
    fun test() = 1
}

// EXPRESSION: (object: T {}).test()
// RESULT: 1: I

// EXPRESSION: (object: T { fun a() = 1 }).a()
// RESULT: 1: I

// EXPRESSION: object: T {}
// RESULT: instance of ceObject.DebugFileKt$generated_for_debugger_kotlin_rulezzzz$1(id=ID): LceObject/DebugFileKt$generated_for_debugger_kotlin_rulezzzz$1;
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
// RESULT: instance of Generated_for_debugger_class$generated_for_debugger_fun$1(id=ID): LGenerated_for_debugger_class$generated_for_debugger_fun$1;
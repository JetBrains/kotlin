package codegen.function.defaultsWithVarArg1

import kotlin.test.*

fun foo(s: String = "", vararg args: Any) {
    if (args == null) {
        println("Failed!")
    } else {
        print("$s ")
        args.forEach {
            print("$it")
        }
        println(", Correct!")
    }
}

@Test fun runTest() {
    foo("Hello")
    foo("Hello", "World")
    foo()
}
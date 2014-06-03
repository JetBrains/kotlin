package privateMember

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

class MyClass {
    private fun privateFun() = 1
    private val privateVal = 1

    private class PrivateClass {
        val a = 1
    }
}

// EXPRESSION: MyClass().privateFun()
// RESULT: 1: I

// EXPRESSION: MyClass().privateVal
// RESULT: 1: I

// EXPRESSION: MyClass.PrivateClass().a
// RESULT: 1: I
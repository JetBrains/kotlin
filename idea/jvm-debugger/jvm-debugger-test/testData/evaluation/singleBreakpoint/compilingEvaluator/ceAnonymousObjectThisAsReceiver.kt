package ceAnonymousObjectThisAsReceiver

fun main(args : Array<String>) {
    val localObject = object {
        fun test() = 1

        fun foo() {
            //Breakpoint!
            val a = 1
        }
    }

    localObject.foo()
}

// EXPRESSION: test()
// RESULT: 1: I

// EXPRESSION: this.test()
// RESULT: 1: I
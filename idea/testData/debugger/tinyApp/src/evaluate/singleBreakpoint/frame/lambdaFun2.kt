package lambdaFun2

class Foo {
    fun foo() {
        block {
            //Breakpoint!
            val a = this@Foo
        }
    }
}

fun <T> block(block: () -> T): T {
    return block()
}

fun main() {
    Foo().foo()
}

// PRINT_FRAME
// SHOW_KOTLIN_VARIABLES

// EXPRESSION: this
// RESULT: instance of lambdaFun2.Foo(id=ID): LlambdaFun2/Foo;
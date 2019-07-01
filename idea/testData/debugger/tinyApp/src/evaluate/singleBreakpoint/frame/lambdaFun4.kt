package lambdaFun4

interface Foo {
    fun foo() {
        block {
            with ("abc") {
                //Breakpoint!
                val a = this@Foo
            }
        }
    }
}

fun <T> block(block: () -> T): T {
    return block()
}

fun main() {
    (object : Foo {}).foo()
}

// PRINT_FRAME
// SHOW_KOTLIN_VARIABLES

// EXPRESSION: this
// RESULT: "abc": Ljava/lang/String;

// EXPRESSION: this@Foo
// RESULT: instance of lambdaFun4.LambdaFun4Kt$main$1(id=ID): LlambdaFun4/LambdaFun4Kt$main$1;
package field

class Foo {
    val field = "OK"
    fun foo() {
        //Breakpoint!
        5
    }
}

fun main() {
    Foo().foo()
}

// EXPRESSION: field
// RESULT: "OK": Ljava/lang/String;

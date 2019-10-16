package valueParameterName

fun main() {
    "foo".foo()
}

fun String.foo() {
    val a = Foo(
        a = this
    )
    val b = Foo(
        //Breakpoint!
        a = this
    )
}

private class Foo(val a: String)

// EXPRESSION: this
// RESULT: "foo": Ljava/lang/String;
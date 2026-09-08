annotation class A(val a: Int, val c: String)

fun foo() {
    val x = @A(42, "42") (label@ ({<caret>
            "Hi"
    }))
}

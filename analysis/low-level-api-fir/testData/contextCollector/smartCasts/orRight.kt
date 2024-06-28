interface Foo {
    val isValid: Boolean
}

fun test(obj: Any) {
    if (obj is Foo || <expr>isValid(obj)</expr>) {
        consume(obj)
    }
}

fun isValid(obj: Any): Boolean = true

fun consume(obj: Foo) {}
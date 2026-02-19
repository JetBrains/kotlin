interface Foo {
    val isValid: Boolean
}

fun test(obj: Any) {
    if (obj is Foo && <expr>obj.isValid</expr>) {
        consume(obj)
    }
}

fun consume(obj: Foo) {}
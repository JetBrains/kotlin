fun test() {
    buildFoo {
        value = <expr>produceString()</expr>
    }
}

fun buildFoo(builder: Foo.() -> Unit): Foo {
    val foo = Foo()
    foo.builder()
    return foo
}

fun Foo {
    var value: String? = null
}

fun produceString(): String = ""
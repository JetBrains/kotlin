fun test() {
    buildFoo {
        <expr>value</expr> = produceString()
    }
}

fun buildFoo(builder: Foo.() -> Unit): Foo {
    val foo = Foo()
    foo.builder()
    return foo
}

class Foo {
    var value: String? = null
}

fun produceString(): String = ""
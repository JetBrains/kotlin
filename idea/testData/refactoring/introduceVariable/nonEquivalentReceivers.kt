class A {
    val value : Int = 0
}

fun foo(body: A.() -> Unit) {}

fun bar() {
    foo {
        print(<selection>value</selection>)
        print(value)
    }

    foo {
        print(value)
        print(value)
    }
}
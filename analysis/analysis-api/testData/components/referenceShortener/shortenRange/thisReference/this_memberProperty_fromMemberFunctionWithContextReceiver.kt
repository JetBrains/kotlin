package test

class Foo {
    val one: Int = 10

    context(Context)
    fun usage() {
        <expr>this.one</expr>
    }
}

class Context {
    val one: Int = 20
}

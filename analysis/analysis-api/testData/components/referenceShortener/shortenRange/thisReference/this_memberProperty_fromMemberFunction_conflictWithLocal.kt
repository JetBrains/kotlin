package test

class Foo {
    val one: Int = 10

    fun usage() {
        val one = 20

        <expr>this.one</expr>
    }
}

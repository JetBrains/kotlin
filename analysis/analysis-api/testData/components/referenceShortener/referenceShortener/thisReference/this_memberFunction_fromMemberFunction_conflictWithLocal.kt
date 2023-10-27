package test

class Foo {
    fun one() {}

    fun usage() {
        fun one() {}

        <expr>this.one()</expr>
    }
}

class Sub : JavaSuper() {
    override fun foo(a: Int) {
    }

    override fun foo() {
    }
}

fun main(args: Array<String>) {
    JavaSuper().foo()
    JavaSuper().foo(1)
    Sub().foo()
    Sub().foo(1)
}
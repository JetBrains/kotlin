// WITH_RUNTIME

class A {
    val s: String

    fun foo() {
        boo()
    }

    fun boo() {
        too()
    }

    fun too() {
        <!LEAKING_THIS!>s<!>.length
    }

    init {
        foo()
        s = ""
    }
}

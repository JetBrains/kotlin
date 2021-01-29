// WITH_RUNTIME

class A {
    val s: String
    var t: Int

    fun foo() {
        t = 1
        boo()
    }

    fun boo() {
        t = 2
        too()
    }

    fun too() {
        t.toLong()
        <!MAY_BE_NOT_INITIALIZED!>s<!>.length
    }

    init {
        <!LEAKING_THIS!>foo()<!>
        s = ""
    }
}

class A {
    fun foo() {}
}

fun <T> T.with(block: T.() -> Unit) {}

fun Any?.test_1() {
    if (this is A) {
        this.foo()
        foo()
    } else {
        this.foo()
        foo()
    }
    this.foo()
    foo()
}

fun Any?.test_2() {
    if (this !is A) {
        this.foo()
        foo()
    } else {
        this.foo()
        foo()
    }
    this.foo()
    foo()
}


fun test_3(a: Any, b: Any, c: Any) {
    with(a) wa@{
        with(b) wb@{
            with(c) wc@{
                this@wb as A
                this@wb.foo()
                foo()
            }
            this.foo()
            foo()
        }
    }
}
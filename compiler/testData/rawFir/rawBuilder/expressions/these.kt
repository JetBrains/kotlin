class Some {
    fun foo(): Int = 1

    fun bar(): Int {
        return this.foo()
    }

    val instance: Some
        get() = this@Some

    fun String.extension(): Int {
        return this@Some.bar() + this.length
    }
}

fun Some.extension() = this.bar()

fun test(some: Some): Int {
    return with(some) {
        this.foo() + this@with.extension()
    }
}
// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

inline class Foo(val x: Int) {
    fun empty() = ""
    fun withParam(a: String) = a
    fun withInlineClassParam(f: Foo) = f.toString()

    fun test(): String {
        val a = empty()
        val b = withParam("hello")
        val c = withInlineClassParam(this)
        return a + b + c
    }

    override fun toString(): String {
        return x.toString()
    }
}

fun box(): String {
    val f = Foo(12)
    return if (f.test() != "hello12") "fail" else "OK"
    return "OK"
}
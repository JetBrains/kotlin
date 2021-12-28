// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class Foo<T: Int>(val x: T) {
    fun empty() = ""
    fun withParam(a: String) = a
    fun withInlineClassParam(f: Foo<T>) = f.toString()

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
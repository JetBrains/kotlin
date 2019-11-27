// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

interface A {
    fun foo(): Collection<String>
}

interface B : A {
    override fun foo(): MutableCollection<String>
}

class C : B {
    override fun foo(): MutableList<String> = ArrayList(listOf("C"))
}

fun box(): String {
    val c = C()
    var r = c.foo().iterator().next()
    val b: B = c
    val a: A = c
    r += b.foo().iterator().next()
    r += a.foo().iterator().next()
    return if (r == "CCC") "OK" else "Fail: $r"
}

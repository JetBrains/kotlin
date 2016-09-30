// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS
import java.util.Arrays

interface A {
    fun foo(): Collection<String>
}

interface B : A {
    override fun foo(): MutableCollection<String>
}

class C : B {
    override fun foo(): MutableList<String> = ArrayList(Arrays.asList("C"))
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

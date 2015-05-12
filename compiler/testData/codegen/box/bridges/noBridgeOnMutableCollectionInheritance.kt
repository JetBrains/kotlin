import java.util.ArrayList
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
    r += (c : B).foo().iterator().next()
    r += (c : A).foo().iterator().next()
    return if (r == "CCC") "OK" else "Fail: $r"
}

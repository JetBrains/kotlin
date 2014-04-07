import java.util.ArrayList
import java.util.Arrays

abstract class A {
    abstract fun foo(): List<String>
}

trait B {
    fun foo(): ArrayList<String> = ArrayList(Arrays.asList("B"))
}

open class C : A(), B

trait D {
    fun foo(): Collection<String>
}

class E : D, C()

fun box(): String {
    val e = E()
    var r = e.foo()[0]
    r += (e : D).foo().iterator().next()
    r += (e : C).foo()[0]
    r += (e : B).foo()[0]
    r += (e : A).foo()[0]
    return if (r == "BBBBB") "OK" else "Fail: $r"
}

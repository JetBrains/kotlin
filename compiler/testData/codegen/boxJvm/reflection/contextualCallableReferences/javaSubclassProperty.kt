// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM
// WITH_REFLECT
// ISSUE: KT-86452

// FILE: J.java
public class J extends KBase {
    public J(String tag) {
        super(tag);
    }
}

// FILE: test.kt
import kotlin.reflect.KParameter
import kotlin.test.assertEquals

open class KBase(val tag: String) {
    var storage = ""

    context(c: String)
    val prop: String get() = tag + c

    context(c: String)
    var mutable: String
        get() = storage
        set(value) {
            storage = c + value
        }
}

fun box(): String {
    context("ctx") {
        val unbound = J::prop
        assertEquals(listOf(KParameter.Kind.INSTANCE), unbound.parameters.map { it.kind })
        assertEquals("tctx", unbound.get(J("t")))
        assertEquals("tctx", unbound.call(J("t")))

        val bound = J("b")::prop
        assertEquals(0, bound.parameters.size)
        assertEquals("bctx", bound.get())
        assertEquals("bctx", bound.call())

        val unboundMutable = J::mutable
        val receiver = J("m")
        assertEquals(listOf(KParameter.Kind.INSTANCE, KParameter.Kind.VALUE), unboundMutable.setter.parameters.map { it.kind })
        unboundMutable.set(receiver, "V")
        assertEquals("ctxV", unboundMutable.get(receiver))

        val boundMutable = J("n")::mutable
        boundMutable.set("W")
        assertEquals("ctxW", boundMutable.get())
        assertEquals("ctxW", boundMutable.getter.call())
    }
    return "OK"
}

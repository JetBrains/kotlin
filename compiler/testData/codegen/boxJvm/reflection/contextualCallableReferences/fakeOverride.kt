// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM
// WITH_REFLECT
// ISSUE: KT-86452

import kotlin.reflect.KProperty
import kotlin.test.assertEquals

open class Base {
    context(c: String)
    open fun f(): String = "base-$c"

    context(c: String)
    open val p: String get() = "basep-$c"
}

class Derived : Base() {
    context(c: String)
    override fun f(): String = "derived-$c"
}

fun box(): String {
    context("ctx") {
        val viaBase = Base::f
        assertEquals("derived-ctx", viaBase.call(Derived()))
        assertEquals("base-ctx", viaBase.call(Base()))

        val bound = Derived()::f
        assertEquals("derived-ctx", bound.call())

        val prop = Derived::p
        assertEquals("basep-ctx", prop.call(Derived()))
        assertEquals("basep-ctx", prop.getter.call(Derived()))
    }

    val enumerated = Derived::class.members.single { it.name == "p" } as KProperty<*>
    assertEquals("basep-e", enumerated.call(Derived(), "e"))

    return "OK"
}

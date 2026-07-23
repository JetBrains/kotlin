// LANGUAGE: +ContextParameters +CallableReferencesToContextual
// TARGET_BACKEND: JVM_IR
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
        // base-typed reference with a bound context argument: `call` must use virtual dispatch
        val viaBase = Base::f
        assertEquals("derived-ctx", viaBase.call(Derived()))
        assertEquals("base-ctx", viaBase.call(Base()))

        // bound receiver of the derived type
        val bound = Derived()::f
        assertEquals("derived-ctx", bound.call())

        // property fake override referenced through the subtype
        val prop = Derived::p
        assertEquals("basep-ctx", prop.call(Derived()))
        assertEquals("basep-ctx", prop.getter.call(Derived()))
    }

    // fake override enumerated on the subtype: fully unbound, context argument passed explicitly
    val enumerated = Derived::class.members.single { it.name == "p" } as KProperty<*>
    assertEquals("basep-e", enumerated.call(Derived(), "e"))

    return "OK"
}

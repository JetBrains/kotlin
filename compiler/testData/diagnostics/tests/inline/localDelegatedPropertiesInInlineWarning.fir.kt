// !DIAGNOSTICS: -NOTHING_TO_INLINE
// LANGUAGE: -ForbidLocalDelegatedPropertiesWithPrivateAccessorsInPublicInlineFunctions

import kotlin.reflect.KProperty

@PublishedApi
internal class A {
    internal operator fun getValue(nothing: Any?, property: KProperty<*>): String = ""
}

@PublishedApi
internal class B {
    operator fun getValue(nothing: Any?, property: KProperty<*>): String = ""
}

class C {
    internal operator fun getValue(nothing: Nothing?, property: KProperty<*>): String = ""
}

class D {
    @PublishedApi
    internal operator fun getValue(nothing: Nothing?, property: KProperty<*>): String = ""
}

class E {
    operator fun getValue(nothing: Nothing?, property: KProperty<*>): String = ""
}

private operator fun Int.getValue(nothing: Any?, property: KProperty<*>) = ""
internal operator fun Boolean.getValue(nothing: Any?, property: KProperty<*>) = ""
operator fun Double.getValue(nothing: Any?, property: KProperty<*>) = ""

@PublishedApi
internal operator fun Char.getValue(nothing: Any?, property: KProperty<*>) = ""

private operator fun String.setValue(nothing: Any?, property: KProperty<*>, any: Any) = "OK"
operator fun String.getValue(nothing: Any?, property: KProperty<*>) = "OK"

private inline fun privateInlineFun() {
    val a by A()
    val b by B()
    val c by C()
    val d by D()
    val e by E()
    val f by 1
    val g by true
    val h by 1.0
    val i by 'a'
    var j by ""
    val k by ""
}

internal inline fun internalInlineFun() {
    val a by A()
    val b by B()
    val c by C()
    val d by D()
    val e by E()
    val f by 1
    val g by true
    val h by 1.0
    val i by 'a'
    var j by ""
    val k by ""
}

inline fun publicInlineFun() {
    val a by <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>A<!>()
    val b by B()
    val c by <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>C<!>()
    val d by D()
    val e by E()
    val f by <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>1<!>
    val g by <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>true<!>
    val h by 1.0
    val i by 'a'
    var j by <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>""<!>
    val k by ""
}

@PublishedApi
internal inline fun publishedApiInlineFun() {
    val a by <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>A<!>()
    val b by B()
    val c by <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>C<!>()
    val d by D()
    val e by E()
    val f by <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>1<!>
    val g by <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>true<!>
    val h by 1.0
    val i by 'a'
    var j by <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>""<!>
    val k by ""
}


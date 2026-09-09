// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89232

open class Base {
    companion object {
        val Common: Base = Base()
        val ONLY_IN_BASE: Base = Base()
    }
}

class Derived : Base() {
    companion object {
        val Common: Derived = Derived()
        val ONLY_IN_DERIVED: Derived = Derived()
    }
}

class Other : Base() {
    companion object {
        val ONLY_IN_OTHER: Other = Other()
    }
}

interface Marker {
    companion object {
        val Common: Marker = object : Marker {}
    }
}

class DerivedMarker : Base(), Marker

sealed class Sealed {
    object Common : Sealed()

    class D1 : Sealed()
    class D2 : Sealed()
}

fun <T> select(x: T, y: T): T = x
fun <T> select3(x: T, y: T, z: T): T = x

fun test(d: Derived, o: Other, dm: DerivedMarker, d1: Sealed.D1, d2: Sealed.D2) {
    // Derived <: T <: Base: Derived is the most specific bound, its Common wins over Base.Common
    val a: Base = select(d, <!UNRESOLVED_REFERENCE!>Common<!>)
    val b: Base = select(<!UNRESOLVED_REFERENCE!>Common<!>, d)
    val c: Base = select(d, ONLY_IN_BASE)
    val e: Base = select(d, ONLY_IN_DERIVED)

    // Derived <: T, Other <: T, T <: Base: Base is not the most specific, Derived and Other are unrelated
    val f: Base = select3(d, o, <!UNRESOLVED_REFERENCE!>Common<!>)
    val g: Base = select3(d, o, ONLY_IN_OTHER)
    val h: Base = select3(d, o, ONLY_IN_BASE)

    // DerivedMarker <: T, T <: Base, T <: Marker: the most specific bound is a class implementing the interface bound
    val i: Base = select(dm, Common)
    val j = takeBaseMarker(dm, <!UNRESOLVED_REFERENCE!>Common<!>)

    // D1 <: T, D2 <: T, T <: Sealed: resolved through the sealed parent in the same way for both
    val k: Sealed = select3(d1, d2, Common)
}

fun <T> takeBaseMarker(x: T, y: T): T where T : Base, T : Marker = x

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, companionObject, functionDeclaration,
interfaceDeclaration, localProperty, nestedClass, nullableType, objectDeclaration, propertyDeclaration, sealed,
typeConstraint, typeParameter */

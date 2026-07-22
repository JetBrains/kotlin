// RUN_PIPELINE_TILL: BACKEND

// MODULE: lib

// FILE: Regular.kt

open class Regular {
    val r: Int get() = 42
    override fun equals(@EqualityBound(Regular::class) other: Any?): Boolean = true
}

open class Inherited : Regular() {
    override fun equals(other: Any?): Boolean = this.r == other.r
}

interface H

interface I : H {
    fun i()
    override fun equals(@EqualityBound(I::class) other: Any?): Boolean
}

abstract class A {
    abstract fun a()
    abstract override fun equals(@EqualityBound(A::class) other: Any?): Boolean
}

class Generic<X : CharSequence> {
    val x: X get() = null!!
    override fun equals(@EqualityBound(Generic::class) other: Any?): Boolean = true
}

interface J : H {
    override fun equals(@EqualityBound(H::class) other: Any?): Boolean
}

interface IJ : I, J
interface JI : J, I

// MODULE: main(lib)

// FILE: Main.kt

class Child : Regular()
class ChildOverriding : Regular() {
    override fun equals(other: Any?): Boolean = true
}

class InheritedChild : Inherited()
class InheritedChildOverriding : Inherited() {
    override fun equals(other: Any?): Boolean = this.r == other.r
}

fun useSite_1(x: Regular?, y: Any?) {
    if (x == y) y?.r
    if (x != null && x == y) y.r
}

fun useSite_2(x: Child?, y: Any) {
    if (x == y) y.r
}

fun useSite_3(x: ChildOverriding?, y: Any?) {
    if (x == y) y?.r
    if (x != null && x == y) y.r
}

fun useSite_4(x: Inherited?, y: Any) {
    if (x == y) y.r
}

fun useSite_5(x: InheritedChild, y: Any?) {
    if (x == y) y.r
}

fun useSite_6(x: Any?, y: Any?) {
    if (x is InheritedChildOverriding && x == y) y.r
}

inline fun <reified T> useSite_7(x: Any?, y: Any) where T : I, T : A {
    if (x is I? && x is A? && x == y) {
        y.i()
        y.a()
    }
    if (x is A? && x is I? && x == y) {
        y.a()
        y.i()
    }
    x as T?
    if (x != y) return
    y.a()
    y.i()
}

fun useSite_8(x: Generic<*>, y: Generic<String>, z: Any?) {
    if (x == z || y == z) {
        z.x.length
    }
}

fun useSite_9(ij: IJ, ji: JI, z: Any) {
    if (ij == z) z.i()
    if (ji == z) z.i()
}

/* GENERATED_FIR_TAGS: classDeclaration, classReference, equalityExpression, functionDeclaration, getter, ifExpression,
integerLiteral, nullableType, operator, override, propertyDeclaration, safeCall, smartcast */

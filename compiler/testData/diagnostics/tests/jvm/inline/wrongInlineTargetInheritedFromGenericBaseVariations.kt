// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_VARIABLE -UNUSED_PARAMETER -UNUSED_EXPRESSION -FINAL_UPPER_BOUND
// ISSUE: KT-90553

// MODULE: base11
// JVM_TARGET: 11
// FILE: base11.kt
package lib

import kotlin.reflect.KProperty

open class Base11<T> {
    inline fun inlineFunction(value: T): T = value
    inline fun plain(): Int = 11
    inline fun <reified R> reified(): String = R::class.java.name
    inline fun withLambda(block: (T) -> Unit) {}
    inline fun String.memberExt(): String = this
    var inlineSetter: Int
        get() = 11
        inline set(v) {}
}

open class OpBase11<T> {
    inline operator fun plus(other: T): Int = 11
    inline operator fun get(index: Int): Int = 11
    inline operator fun invoke(): Int = 11
    inline operator fun component1(): Int = 11
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = 11
    inline operator fun iterator(): Iterator<T> = emptyList<T>().iterator()
}

inline fun <T> Base11<T>.extOnBase(): Int = 11

// MODULE: base17
// JVM_TARGET: 17
// FILE: base17.kt
package lib

import kotlin.reflect.KProperty

open class Base17<T> {
    inline fun inlineFunction(value: T): T = value
    inline fun plain(): Int = 17
    inline fun <reified R> reified(): String = R::class.java.name
    inline fun withLambda(block: (T) -> Unit) {}
    inline fun String.memberExt(): String = this
    var inlineSetter: Int
        get() = 17
        inline set(v) {}
}

open class OpBase17<T> {
    inline operator fun plus(other: T): Int = 17
    inline operator fun get(index: Int): Int = 17
    inline operator fun invoke(): Int = 17
    inline operator fun component1(): Int = 17
    inline operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = 17
    inline operator fun iterator(): Iterator<T> = emptyList<T>().iterator()
}

open class NonGeneric17 {
    inline fun inlineFunction(value: String): String = value
    inline val inlineProperty: Int
        get() = 17
}

open class GenMethod17 {
    inline fun <R> inlineFunction(value: R): R = value
}

open class GrandParent17 {
    inline fun gpFunction(): Int = 17
}

open class GenParent17<T> : GrandParent17()

inline fun <T> Base17<T>.extOnBase(): Int = 17

inline fun topLevel17(value: String): String = value

// MODULE: derived17(base11)
// JVM_TARGET: 17
// FILE: derived17.kt
package lib

class Derived17 : Base11<String>()
class OpDerived17 : OpBase11<String>()
class GenDerived17<T> : Base11<T>()
class NestedArg17<T> : Base11<List<T>>()
object Obj17 : Base11<String>()
class Holder17 { companion object : Base11<String>() }
open class OpenDerived17 : Base11<String>()

// MODULE: derived11(base17)
// JVM_TARGET: 11
// FILE: derived11.kt
package lib

class Derived11 : Base17<String>()
class OpDerived11 : OpBase17<String>()
class GenDerived11<T> : Base17<T>()
class NestedArg11<T> : Base17<List<T>>()
object Obj11 : Base17<String>()
class Holder11 { companion object : Base17<String>() }
open class OpenDerived11 : Base17<String>()
class NonGenericDerived11 : NonGeneric17()
class GenMethodDerived11 : GenMethod17()
class GpDerived11 : GenParent17<String>()

// MODULE: leaf17(base17, derived11)
// JVM_TARGET: 17
// FILE: leaf17.kt
package lib

class Leaf17 : OpenDerived11()

// MODULE: leaf11(base11, derived17)
// JVM_TARGET: 11
// FILE: leaf11.kt
package lib

class Leaf11 : OpenDerived17()

// MODULE: javaDerived(base11, base17)
// FILE: lib/JDerived11.java
package lib;

public class JDerived11 extends Base17<String> {}

// FILE: lib/JDerived17.java
package lib;

public class JDerived17 extends Base11<String> {}

// MODULE: main(base11, base17, derived17, derived11, leaf17, leaf11, javaDerived)
// JVM_TARGET: 11
// FILE: main.kt
package main

import lib.*

fun memberKinds(d17: Derived17, d11: Derived11) {
    d17.plain()
    d11.<!INLINE_FROM_HIGHER_PLATFORM!>plain<!>()
    d17.reified<Int>()
    d11.<!INLINE_FROM_HIGHER_PLATFORM!>reified<!><Int>()
    d17.withLambda { }
    d11.<!INLINE_FROM_HIGHER_PLATFORM!>withLambda<!> { }
    with(d17) { "s".memberExt() }
    with(d11) { "s".<!INLINE_FROM_HIGHER_PLATFORM!>memberExt<!>() }
    d17.inlineSetter = 1
    d11.<!INLINE_FROM_HIGHER_PLATFORM!>inlineSetter<!> = 1
    d11.inlineSetter
}

fun operators(o17: OpDerived17, o11: OpDerived11, opBase17: OpBase17<String>) {
    o17 + "x"
    o11 <!INLINE_FROM_HIGHER_PLATFORM!>+<!> "x"
    o17[0]
    <!INLINE_FROM_HIGHER_PLATFORM!>o11[0]<!>
    o17()
    <!INLINE_FROM_HIGHER_PLATFORM!>o11<!>()
    val (a) = o17
    val (<!INLINE_FROM_HIGHER_PLATFORM!>b<!>) = o11
    val c by o17
    val d by <!INLINE_FROM_HIGHER_PLATFORM!>o11<!>
    val e by <!INLINE_FROM_HIGHER_PLATFORM!>opBase17<!>
    for (x in o17) {}
    for (x in <!INLINE_FROM_HIGHER_PLATFORM!>o11<!>) {}
    for (x in <!INLINE_FROM_HIGHER_PLATFORM!>opBase17<!>) {}
}

fun receiverForms(d17: Derived17, d11: Derived11, n17: Derived17?, n11: Derived11?, a: Any, b17: Base17<String>) {
    with(d17) { inlineFunction("x") }
    with(d11) { <!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x") }
    n17?.inlineFunction("x")
    n11?.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
    if (a is Derived17) a.inlineFunction("x")
    if (a is Derived11) a.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
    if (b17 is Derived11) b17.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
    d17::inlineFunction
    d11::<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>
    (d17 as Base11<String>).inlineFunction("x")
    (d11 as Base17<String>).<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
}

fun <D11 : Derived11> typeParameterDerived(d11: D11) {
    d11.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
}

fun <B17 : Base17<String>> typeParameterBase(b17: B17) {
    b17.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
}

fun subclassShapes(g17: GenDerived17<String>, g11: GenDerived11<String>, na17: NestedArg17<Int>, na11: NestedArg11<Int>) {
    g17.inlineFunction("x")
    g11.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
    na17.inlineFunction(listOf(1))
    na11.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>(listOf(1))
    Obj17.inlineFunction("x")
    Obj11.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
    Holder17.inlineFunction("x")
    Holder11.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
}

fun intermediateClass(leaf17: Leaf17, leaf11: Leaf11) {
    leaf17.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
    leaf11.inlineFunction("x")
}

fun javaSubclass(j11: JDerived11, j17: JDerived17) {
    j11.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
    j17.inlineFunction("x")
}

class Src11 : Base17<String>() {
    fun inside() = <!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
}

class SrcOk11 : Base11<String>() {
    fun inside() = inlineFunction("x")
}

fun sourceSubclass(src11: Src11, srcOk11: SrcOk11) {
    src11.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
    srcOk11.inlineFunction("x")
    object : Base17<String>() {}.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
}

fun nonGenericDeclaringClass(ng11: NonGenericDerived11, m11: GenMethodDerived11, gp11: GpDerived11) {
    ng11.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
    ng11.<!INLINE_FROM_HIGHER_PLATFORM!>inlineProperty<!>
    m11.<!INLINE_FROM_HIGHER_PLATFORM!>inlineFunction<!>("x")
    gp11.<!INLINE_FROM_HIGHER_PLATFORM!>gpFunction<!>()
}

fun topLevel(d17: Derived17, d11: Derived11) {
    <!INLINE_FROM_HIGHER_PLATFORM!>topLevel17<!>("x")
    d11.<!INLINE_FROM_HIGHER_PLATFORM!>extOnBase<!>()
    d17.extOnBase()
}

/* GENERATED_FIR_TAGS: additiveExpression, anonymousObjectExpression, asExpression, assignment, callableReference,
classDeclaration, classReference, companionObject, destructuringDeclaration, dnnType, flexibleType, forLoop,
funWithExtensionReceiver, functionDeclaration, functionalType, getter, ifExpression, inline, integerLiteral,
isExpression, javaProperty, lambdaLiteral, localProperty, nullableType, objectDeclaration, operator, propertyDeclaration,
propertyDelegate, reified, safeCall, setter, smartcast, starProjection, stringLiteral, thisExpression, typeConstraint,
typeParameter */

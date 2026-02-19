// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-58013 (related)
// WITH_REFLECT
// FIR_DUMP

import kotlin.reflect.KProperty

data class Ref<D>(val t: D)

operator fun <V> Ref<V>.getValue(hisRef: Any?, property: KProperty<*>): V = this.t

fun <E> List<Ref<*>>.getElement(i: Int): Ref<E> = this[i] <!UNCHECKED_CAST!>as Ref<E><!>

fun test(list: List<Ref<*>>) {
    val data: String by try { list.getElement(0) } finally {}
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, data, funWithExtensionReceiver, functionDeclaration,
integerLiteral, localProperty, nullableType, operator, primaryConstructor, propertyDeclaration, propertyDelegate,
starProjection, thisExpression, tryExpression, typeParameter */

// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-58013
// WITH_REFLECT
// FIR_DUMP

import kotlin.reflect.KProperty

data class Ref<D>(val t: D)

class GenericDelegate<G>(val value: G)

operator fun <V> Ref<V>.provideDelegate(a: Any?, p: KProperty<*>): GenericDelegate<V> = GenericDelegate(this.t)

operator fun <W> GenericDelegate<W>.getValue(a: Any?, p: KProperty<*>): W = this.value

fun <E> List<Ref<*>>.getElement(i: Int): Ref<E> = this[i] <!UNCHECKED_CAST!>as Ref<E><!>

fun test(list: List<Ref<*>>) {
    val data: String by <!DELEGATE_SPECIAL_FUNCTION_MISSING, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>list.getElement(0)<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>

    val data2: String by list.getElement(0)
}

/* GENERATED_FIR_TAGS: asExpression, checkNotNullCall, classDeclaration, data, funWithExtensionReceiver,
functionDeclaration, integerLiteral, localProperty, nullableType, operator, primaryConstructor, propertyDeclaration,
propertyDelegate, starProjection, thisExpression, typeParameter */

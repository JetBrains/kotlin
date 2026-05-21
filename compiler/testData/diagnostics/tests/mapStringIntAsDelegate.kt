// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-86292
// WITH_STDLIB

import kotlin.reflect.KProperty

fun <T> mk(): T = TODO()

class Foo<out T>(val v: T)

operator fun <T, S : T> Foo<T>.getValue(t: Any?, p: KProperty<*>): S =
    @Suppress("UNCHECKED_CAST") (v as S)

fun main() {
    val m: Map<String, Int> = mapOf("d" to 42)
    val f: Foo<Int> = Foo(42)

    val dm: String by <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>m<!>
    val dmD: String = m.<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>getValue<!>(null, mk())

    val df: String by <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>f<!>
    val dfD: String = f.<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>getValue<!>(null, mk())
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral,
intersectionType, localProperty, nullableType, operator, out, primaryConstructor, propertyDeclaration, propertyDelegate,
stringLiteral, typeConstraint, typeParameter */

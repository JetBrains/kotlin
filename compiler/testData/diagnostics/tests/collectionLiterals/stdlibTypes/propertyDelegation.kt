// RUN_PIPELINE_TILL: FRONTEND
// WITH_REFLECT
// ISSUE: KT-84333
// LANGUAGE: +CollectionLiterals

// FILE: getValue.kt

package test.getValue

import kotlin.reflect.KProperty

operator fun List<Int>.getValue(thisRef: Any?, k: KProperty<*>): Int {
    return first()
}

fun test() {
    val x by <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    val y by [42]
}

// FILE: getAndSetValue.kt

package test.getAndSetValue

import kotlin.reflect.KProperty

operator fun <T> List<T>.getValue(thisRef: Nothing?, k: KProperty<*>): T {
    return first()
}

operator fun <T> List<T>.setValue(thisRef: Nothing?, k: KProperty<*>, value: T) {
}

fun test() {
    val x by [1, 2, 3]

    var y by [1, "2", 3]
    y = 1
    y = "2"
    y = '3'

    var z: Char by <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>
    z = 'a'
}

/* GENERATED_FIR_TAGS: assignment, funWithExtensionReceiver, functionDeclaration, integerLiteral, intersectionType,
localProperty, nullableType, operator, propertyDeclaration, propertyDelegate, setter, stringLiteral, typeParameter */

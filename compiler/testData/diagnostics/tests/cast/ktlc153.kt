// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KTLC-153

class Box<T>(val value: T)

fun testSimpleArray(x: Any) {
    x is Array<String>
    x is Array<Int>
    x is Array<Array<String>>
    x as Array<String>
    x as Array<Int>
    x as Array<Array<String>>
}

fun testErasedArray(x: Any) {
    x is <!CANNOT_CHECK_FOR_ERASED!>Array<Box<String>><!>
    x is <!CANNOT_CHECK_FOR_ERASED!>Array<List<String>><!>
    x <!UNCHECKED_CAST!>as Array<Box<String>><!>
    x <!UNCHECKED_CAST!>as Array<List<String>><!>
}

fun testStarProjectionArray(x: Any) {
    x is Array<List<*>>
    x is Array<Box<*>>
    x as Array<List<*>>
    x as Array<Box<*>>
}

fun <T> testTypeParameterArray(x: Any) {
    x is <!CANNOT_CHECK_FOR_ERASED!>Array<T><!>
    x is <!CANNOT_CHECK_FOR_ERASED!>Array<Array<T>><!>
    x <!UNCHECKED_CAST!>as Array<T><!>
}

inline fun <reified T> testReifiedTypeParameterArray(x: Any) {
    x is Array<T>
    x is Array<Array<T>>
    x as Array<T>
    x as Array<Array<T>>
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, inline, intersectionType, isExpression,
nullableType, primaryConstructor, propertyDeclaration, reified, smartcast, starProjection, typeParameter */

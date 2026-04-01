// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun main() {
    val x: Any = emptyList<Int>()
    test_1(x)
    test_2(x)
}

fun <T> test_1(obj: T) {
    if (obj == arrayListOf<Int>()) {
        // ClassCastException at runtime
        // should be no smartcast
        obj.<!UNRESOLVED_REFERENCE!>ensureCapacity<!>(100)
    }
}

fun test_2(obj: Any) {
    if (obj == arrayListOf<Int>()) {
        // no smartcast as expected
        obj.<!UNRESOLVED_REFERENCE!>ensureCapacity<!>(100)
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, intersectionType,
localProperty, nullableType, propertyDeclaration, smartcast, typeParameter */

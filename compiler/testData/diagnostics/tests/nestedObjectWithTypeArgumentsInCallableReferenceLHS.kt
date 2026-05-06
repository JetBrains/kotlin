// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66344

class Outer<T> {
    object Nested
}

fun main() {
    <!TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED!>Outer<Int>.Nested<!>::<!UNRESOLVED_REFERENCE!>toString<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass, nullableType, objectDeclaration,
typeParameter */

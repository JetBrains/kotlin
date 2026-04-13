// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-59574

// KT-59574: "Unresolved reference" on method reference with nullable receiver in if expression

fun test(y: Comparator<Double>?) {
    val res = if (true)
        y::<!UNSAFE_CALLABLE_REFERENCE!>compare<!>
    else TODO()
}

interface Comparator<T> {
    fun compare(o1: T, o2: T): Int
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, interfaceDeclaration, localProperty, nullableType,
propertyDeclaration, typeParameter */

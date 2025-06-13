// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-62819

class A<T>

fun foo(cond: Boolean) {
    val first = when (cond) {
        true -> A<Int>()
        false -> A<String?>()
    }

    val second = when (cond) {
        true -> first
        false -> first
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, intersectionType, localProperty,
nullableType, outProjection, propertyDeclaration, smartcast, typeParameter, whenExpression, whenWithSubject */

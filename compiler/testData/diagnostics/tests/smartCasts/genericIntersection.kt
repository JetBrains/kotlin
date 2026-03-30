// RUN_PIPELINE_TILL: BACKEND
// See also KT-7801
class A

fun <T> test(v: T): T {
    val a: T = if (v !is A) v else v
    return a
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, intersectionType, isExpression,
localProperty, nullableType, propertyDeclaration, smartcast, typeParameter */

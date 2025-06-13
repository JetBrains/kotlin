// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER

interface A
interface B {
    fun test() {}
}

fun <K> select(a: K, b: K): K = a

fun test(a: A?, b: B?) {
    b as A?
    a as B?
    val c = select(a, b)
    if (c != null) {
        c.test()
    }
}

/* GENERATED_FIR_TAGS: asExpression, equalityExpression, functionDeclaration, ifExpression, interfaceDeclaration,
intersectionType, localProperty, nullableType, propertyDeclaration, smartcast, typeParameter */

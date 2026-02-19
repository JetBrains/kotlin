// RUN_PIPELINE_TILL: BACKEND
interface A<out T>

interface MutableA<T> : A<T> {
    fun add(x: T)
}

interface MutableString : MutableA<String>

fun test(a: A<String>) {
    (a as? MutableA)?.add("")
    (a as MutableA).add("")
}

fun test2(a: A<String>) {
    val b = a as MutableString
    b.add("")
}

fun test3(a: A<String>) {
    if (a is MutableA) {
        a.add("")
    }
}

/* GENERATED_FIR_TAGS: asExpression, functionDeclaration, ifExpression, interfaceDeclaration, isExpression,
localProperty, nullableType, out, propertyDeclaration, safeCall, smartcast, stringLiteral, typeParameter */

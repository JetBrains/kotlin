// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_VARIABLE

interface A {
    operator fun getValue(x: Any?, y: Any?): Any?
}

interface B : A {
    override fun getValue(x: Any?, y: Any?): Int
}

fun test(a: A) {
    if (a is B) {
        val x: Int by a
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, interfaceDeclaration, isExpression, localProperty,
nullableType, operator, override, propertyDeclaration, propertyDelegate, smartcast */

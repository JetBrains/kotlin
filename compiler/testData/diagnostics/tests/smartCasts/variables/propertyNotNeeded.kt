// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class MyClass(var p: String?)

fun bar(s: String?): Int {
    return s?.length ?: -1
}

fun foo(m: MyClass): Int {
    m.p = "xyz"
    return bar(m.p)
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, elvisExpression, functionDeclaration, integerLiteral, nullableType,
primaryConstructor, propertyDeclaration, safeCall, stringLiteral */

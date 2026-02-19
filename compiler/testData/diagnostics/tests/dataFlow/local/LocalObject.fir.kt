// RUN_PIPELINE_TILL: BACKEND
fun foo(x: Any?) {
    if (x is String) {
        object : Base(x) {
            fun bar() = x.length
        }
    }
}

open class Base(s: String)

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, ifExpression, isExpression,
nullableType, primaryConstructor, smartcast */

// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
interface A {
    fun foo()
}

fun takeA(a: A): Boolean = true

fun test(a: Any) {
    if (takeA(a as? A ?: return)) {
        a.foo()
    }
}

/* GENERATED_FIR_TAGS: elvisExpression, functionDeclaration, ifExpression, interfaceDeclaration, smartcast */

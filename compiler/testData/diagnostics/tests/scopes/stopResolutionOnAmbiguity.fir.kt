// RUN_PIPELINE_TILL: FRONTEND
package c

interface B {
    fun bar() {}
}

class C() {
    fun bar() {
    }
}

fun test(a : Any?) {
    if (a is B) {
        if (<!IMPOSSIBLE_IS_CHECK_ERROR!>a is C<!>) {
            a.bar();
        }
    }
}

fun Any?.bar() {}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, ifExpression,
interfaceDeclaration, intersectionType, isExpression, nullableType, primaryConstructor, smartcast */

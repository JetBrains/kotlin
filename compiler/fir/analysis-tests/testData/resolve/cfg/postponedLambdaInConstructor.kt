// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG

abstract class A(func: () -> String)

class B(val s: String) : A(s.let { { it } }) {
    fun foo() {
        foo()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, lambdaLiteral, primaryConstructor,
propertyDeclaration */

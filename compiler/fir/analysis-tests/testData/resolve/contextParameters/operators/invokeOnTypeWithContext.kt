// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
class A

class Test1: <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>context(A)()->Unit<!> {
    override fun invoke(p1: A) { }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Test2<!>: <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>context(A)()->Unit<!> {
    context(a: A)
    <!NOTHING_TO_OVERRIDE!>override<!> fun invoke() { }
}

fun usage() {
    Test1()(A())

    with(A()) {
        Test1()()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionDeclarationWithContext, functionalType,
lambdaLiteral, operator, override, typeWithContext */

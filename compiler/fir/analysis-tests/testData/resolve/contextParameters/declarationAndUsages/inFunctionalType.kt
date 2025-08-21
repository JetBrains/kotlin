// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

class A {
    fun foo(a: String): String { return a }
}

context(ctx: A)
fun <A> implicit(): A = ctx

val x: context(A) (String) -> String
    get() = { y: String -> implicit<A>().foo(y) }

var y: context(A) (String) -> String
    get() = { y: String -> implicit<A>().foo(y) }
    set(value) { value(A(), "") }

fun bar(a: context(A)(String) -> String) { }

fun qux(): context(A) (String) -> String {
    return { y: String -> implicit<A>().foo(y) }
}

class ContextInSuperType: <!SUPERTYPE_IS_EXTENSION_OR_CONTEXT_FUNCTION_TYPE!>context(A) (String) -> String<!> {
    override fun invoke(p1: A, p2: String): String {
        return p2
    }
}

fun test(){
    x(A(), "")
    y(A(), "")
    y = { y: String -> implicit<A>().foo(y)  }
    bar { y: String -> implicit<A>().foo(y)  }
    qux()(A(), "")
    ContextInSuperType()(A(), "")
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionDeclarationWithContext, functionalType,
getter, lambdaLiteral, nullableType, operator, override, propertyDeclaration, setter, stringLiteral, typeParameter,
typeWithContext */

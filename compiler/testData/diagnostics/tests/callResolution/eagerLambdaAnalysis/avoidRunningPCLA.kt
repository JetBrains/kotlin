// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis

fun <E1> foo(x: String, block: (MutableCollection<E1>) -> String): Int {
    TODO()
}

@JvmName("foo2")
fun <E2> foo(x: Any, block: (MutableCollection<E2>) -> Any): String {
    TODO()
}

fun <E4> bar(x: Any, block: (MutableCollection<E4>) -> Any): String {
    TODO()
}

@JvmName("bar2")
fun <E3> bar(x: String, block: (MutableCollection<E3>) -> String): Int {
    TODO()
}

fun main(x: String) {
    foo(x) {
        it.add("1")
        "2"
    }

    foo(x) {
        it.add("3")
        <!RETURN_TYPE_MISMATCH!>4<!>
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>bar<!>(x) {
        it.add("5")
        "6"
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>bar<!>(x) {
        it.add("7")
        <!RETURN_TYPE_MISMATCH!>8<!>
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, stringLiteral */

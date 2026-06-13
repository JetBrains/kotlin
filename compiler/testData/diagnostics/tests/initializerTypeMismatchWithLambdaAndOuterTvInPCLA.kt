// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82737
// RENDER_DIAGNOSTIC_ARGUMENTS

class Box<T> {
    fun put(x: T) {}

    var prop: (T, Int) -> Unit = { _, _ -> }
    fun get(lam: (T, Int) -> Unit) {}
}

fun <T> buildBox(block: Box<T>.() -> Unit) = Box<T>().apply(block)

fun tst() {
    buildBox {
        prop <!ASSIGNMENT_TYPE_MISMATCH("(Int, Int) -> Unit; (Int, String) -> Unit")!>=<!> { x: Int, y: String -> }
        put(42)
    }

    buildBox {
        get { <!EXPECTED_PARAMETER_TYPE_MISMATCH("Int; uninferred T (of fun <T> buildBox)")!>x: Int<!>, <!EXPECTED_PARAMETER_TYPE_MISMATCH("String; Int")!>y: String<!> -> }
        put(42)
    }

    buildBox {
        prop <!ASSIGNMENT_TYPE_MISMATCH("(Int, Int) -> Unit; (Int, String) -> Unit")!>=<!> { x, y: String -> }
        put(42)
    }

    buildBox {
        get { <!EXPECTED_PARAMETER_TYPE_MISMATCH("Int; uninferred T (of fun <T> buildBox)")!>x<!>, <!EXPECTED_PARAMETER_TYPE_MISMATCH("String; Int")!>y: String<!> -> }
        put(42)
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
nullableType, propertyDeclaration, typeParameter, typeWithExtension */

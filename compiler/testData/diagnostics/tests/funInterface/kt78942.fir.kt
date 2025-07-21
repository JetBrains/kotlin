// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78942
// RENDER_DIAGNOSTICS_FULL_TEXT
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun interface NoTypeParams {
    fun get(): String
}

fun interface OnlyReturn<T> {
    fun get(): T
}

fun interface ToSame<T> {
    fun get(t: T): T
}

fun interface Different<A, B> {
    fun get(a: A): B
}

fun test() {

    NoTypeParams {
        <!RETURN_TYPE_MISMATCH!>fun(): String = ""<!>
    }

    OnlyReturn<String> {
        <!RETURN_TYPE_MISMATCH!>fun() = ""<!>
    }

    val onlyReturn: OnlyReturn<String> = OnlyReturn lbl@{
        <!RETURN_TYPE_MISMATCH!>fun() = ""<!>
    }

    val savedToLocal: OnlyReturn<String> = OnlyReturn {
        val res: () -> String = { "" }
        <!RETURN_TYPE_MISMATCH!>res<!>
    }

    val toSame: ToSame<String> = ToSame { it ->
        <!RETURN_TYPE_MISMATCH!>fun() = it<!>
    }

    ToSame { it: String ->
        <!RETURN_TYPE_MISMATCH!>fun() = it<!>
    }

    Different<Int, String> {
        <!RETURN_TYPE_MISMATCH!>fun() = it.toString()<!>
    }

    val different: Different<Int, String> = Different {
        <!RETURN_TYPE_MISMATCH!>fun() = it.toString()<!>
    }
}

/* GENERATED_FIR_TAGS: anonymousFunction, funInterface, functionDeclaration, functionalType, interfaceDeclaration,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter */

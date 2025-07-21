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
        <!TYPE_MISMATCH, TYPE_MISMATCH!>fun(): String = ""<!>
    }

    OnlyReturn<String> {
        <!TYPE_MISMATCH!>fun() = <!TYPE_MISMATCH!>""<!><!>
    }

    val onlyReturn: OnlyReturn<String> = OnlyReturn lbl@{
        <!TYPE_MISMATCH!>fun() = <!TYPE_MISMATCH!>""<!><!>
    }

    val savedToLocal: OnlyReturn<String> = OnlyReturn {
        val res: () -> String = { "" }
        <!TYPE_MISMATCH!>res<!>
    }

    val toSame: ToSame<String> = ToSame { it ->
        <!TYPE_MISMATCH, TYPE_MISMATCH!>fun() = it<!>
    }

    ToSame { it: String ->
        <!TYPE_MISMATCH!>fun() = <!TYPE_MISMATCH!>it<!><!>
    }

    Different<Int, String> {
        <!TYPE_MISMATCH!>fun() = <!TYPE_MISMATCH!>it.toString()<!><!>
    }

    val different: Different<Int, String> = Different {
        <!TYPE_MISMATCH!>fun() = <!TYPE_MISMATCH!>it.toString()<!><!>
    }
}

/* GENERATED_FIR_TAGS: anonymousFunction, funInterface, functionDeclaration, functionalType, interfaceDeclaration,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, stringLiteral, typeParameter */

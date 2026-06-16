// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_EXPRESSION -UNREACHABLE_CODE -UNUSED_PARAMETER -RETURN_NOT_ALLOWED

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>test1<!>() = run {
    <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> <!RETURN_TYPE_MISMATCH!>"OK"<!>
}

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>test2<!>() = run {
    fun local(): String {
        return ""
    }
    <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> <!RETURN_TYPE_MISMATCH!>""<!>
}

inline fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> = null!!
fun test3(a: List<String>, b: List<Int>) = a.map {
    if (it.length == 3) <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> <!NULL_FOR_NONNULL_TYPE!>null<!>
    if (it.length == 4) <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> <!RETURN_TYPE_MISMATCH!>""<!>
    if (it.length == 4) <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> <!RETURN_TYPE_MISMATCH!>5<!>
    if (it.length == 4) <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> b
    1
}

fun test4() = run {
    fun test5() {
        return

        <!RETURN_TYPE_MISMATCH!>return@test4<!>

        return <!RETURN_TYPE_MISMATCH!>return@test4<!>

        return <!RETURN_TYPE_MISMATCH!>fun() { return; return@test4 <!RETURN_TYPE_MISMATCH!>""<!> }<!>
    }

    <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE, RETURN_TYPE_MISMATCH!>return<!>
    3
}

val foo: Int
    get() = run {
        if (true) return <!RETURN_TYPE_MISMATCH!>""<!>

        <!RETURN_TYPE_MISMATCH!>return<!>
    }

fun test(): Int = run {
    return <!RETURN_TYPE_MISMATCH!>""<!>
}

/* GENERATED_FIR_TAGS: anonymousFunction, checkNotNullCall, equalityExpression, funWithExtensionReceiver,
functionDeclaration, functionalType, getter, ifExpression, inline, integerLiteral, lambdaLiteral, localFunction,
nullableType, propertyDeclaration, stringLiteral, typeParameter */

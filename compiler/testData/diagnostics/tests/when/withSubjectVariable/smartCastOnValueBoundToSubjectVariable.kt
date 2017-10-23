// !LANGUAGE: +VariableDeclarationInWhenSubject

fun foo(s1: String, s2: String) = s1 + s2

fun test(x: Any?) =
        when (val y = x?.toString()) {
            null -> "null"
            else -> foo(<!DEBUG_INFO_SMARTCAST!>x<!>.toString(), <!DEBUG_INFO_SMARTCAST!>y<!>)
        }
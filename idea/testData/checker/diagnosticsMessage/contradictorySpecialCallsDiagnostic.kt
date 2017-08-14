fun <T : Number> foo(<warning>x</warning>: T, <warning>l</warning>: () -> T) {}

fun testIf(i: Int) {
    foo(i) {
        <error descr="[CONTRADICTION_FOR_SPECIAL_CALL] Result type for 'if' expression cannot be inferred:
should be conformed to: Number
should be a supertype of: String (for parameter 'thenBranch')
">if</error> (true) "" else i
    }
}

fun testElvis(i: Int, s: String?) {
    foo(i) {
        s <error descr="[CONTRADICTION_FOR_SPECIAL_CALL] Result type for 'elvis' expression cannot be inferred:
should be conformed to: Number
should be a supertype of: String (for parameter 'left')
">?:</error> i
    }
}

fun testWhen(i: Int, s: String?) {
    foo(i) {
        <error descr="[CONTRADICTION_FOR_SPECIAL_CALL] Result type for 'when' expression cannot be inferred:
should be conformed to: Number
should be a supertype of: String? (for parameter 'entry0')
">when</error> (true) {
        true -> s
        else -> i
    }
}
}

val test: Int = if (true) <error descr="[CONTRADICTION_FOR_SPECIAL_CALL] Result type for 'when' expression cannot be inferred:
should be conformed to: Int (for parameter 'thenBranch')
should be a supertype of: Nothing? (for parameter 'entry1')
">{
when (2) {
    1 -> 1
    else -> null
}
}</error>
else {
    2
}

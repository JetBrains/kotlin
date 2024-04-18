// FIR_IDENTICAL
// ISSUE: KT-55370

fun interface X<I, R> {
    fun apply(input: I): R
}

val nullable = X { input: Int? -> "PROBLEM" + input }

fun interface Y<I, R> {
    fun apply(input: I & Any): R
}

val nonNullableBroken = <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>object<!> : Y<Int?, String> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun apply(input: Int?): String {
        return "Test $input"
    }
}

val nonNullableCorrect = object : Y<Int?, String> {
    override fun apply(input: Int): String {
        return "Test $input"
    }
}

val nonNullableLambda = Y<Int?, String> { it: Int? -> "Test $it" }

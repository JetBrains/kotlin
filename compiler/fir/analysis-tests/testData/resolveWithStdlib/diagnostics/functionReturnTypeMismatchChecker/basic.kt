// bug: type of the expression in return statement is Char
fun illegalReturnIf(): Char {
    return <!RETURN_TYPE_MISMATCH!>if (1 < 2) 'a' else { 1 }<!>
}

fun foo(): String {
    return <!RETURN_TYPE_MISMATCH!>1<!>
}

fun ok(): Int {
    return 1
}

fun okOneLineFunction(): Int = 10 + 1
fun errorOneLineFunction(): String = <!RETURN_TYPE_MISMATCH!>10 + 1<!>

class A {
    fun bar() {}
}

infix fun (() -> Unit).foo(x: A.() -> Unit) {}

fun okWithLambda(): String {
    {
        return@foo
    } foo {
        bar()
        return@foo
    }

    return ""
}

// no report due bad returns in lambda
fun errorWithLambda(): String {
    {
        return@foo
    } foo {
        bar()
        return@foo <!ARGUMENT_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>10<!>
    }

    return ""
}

fun blockReturnValueTypeMatch1() : Int {
    if (1 > 2)
        return <!RETURN_TYPE_MISMATCH!>1.0<!>
    return <!RETURN_TYPE_MISMATCH!>2.0<!>
}
fun blockReturnValueTypeMatch2() : Int {
    if (1 > 2)
    else return <!RETURN_TYPE_MISMATCH!>1.0<!>
    return <!RETURN_TYPE_MISMATCH!>2.0<!>
}

// Simple one-liners should have only method breakpoint
// Simple = no lambdas on a line
fun foo1() = println() /// M

fun foo2() {} /// M

// Lambdas should be available if present
fun foo3() = run { println() } /// *, L, M, λ

// Code blocks {} are not considered as expressions
fun foo4() { /// M
    println() /// L
} /// L

// And parenthesis as well
fun foo5() = ( /// M
        println() /// L
        )

// For expression-body functions, a line breakpoint should be available
// if there is an expression on the first line
fun foo6() = when (2 + 3) { /// M, L
    5 -> {} /// L
    else -> {} /// L
}

// Line breakpoint should not be displayed for lambda literal results
fun foo7() = { println() } /// M, λ

fun foo8() = (3 + 5).run { /// M, L
    println() /// L
} /// L

// Expressions in default parameter values should be recognized
fun foo9(a: String = readLine()!!) = a /// M, L

// Lambdas in default parameter values also should be recognized
fun foo10(a: () -> Unit = { println() }) { /// *, L, M, λ
    a() /// L
} /// L

// If a default parameter value is not just a lambda, but a function call with a lambda argument,
// there should be a line breakpoint as well
fun foo11(a: String = run { "foo" }) = a /// *, L, M, λ
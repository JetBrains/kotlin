@CompileTimeCalculation
fun fib(n: Int) : Int {
    if (n <= 1) return n
    return fib(n - 1) + fib(n - 2)
}

const val n2 = <!EVALUATED: `1`!>fib(2)<!>
const val n10 = <!EVALUATED: `55`!>fib(10)<!>
const val n18 = <!EVALUATED: `2584`!>fib(18)<!>

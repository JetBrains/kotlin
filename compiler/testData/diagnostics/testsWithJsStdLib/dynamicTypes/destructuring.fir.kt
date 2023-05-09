// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {
    for ((<!WRONG_OPERATION_WITH_DYNAMIC!>x<!>, <!WRONG_OPERATION_WITH_DYNAMIC!>y<!>) in A()) {
        println(x + y)
    }

    bar { (<!WRONG_OPERATION_WITH_DYNAMIC!>x<!>, <!WRONG_OPERATION_WITH_DYNAMIC!>y<!>) ->
        println(x + y)
    }

    val x: dynamic = Any()

    val (<!WRONG_OPERATION_WITH_DYNAMIC!>y<!>, <!WRONG_OPERATION_WITH_DYNAMIC!>z<!>) = x
    println(y + z)
}

class A {
    operator fun iterator(): Iterator<dynamic> = TODO("")
}

fun bar(f: (dynamic) -> Unit): Unit = TODO("")

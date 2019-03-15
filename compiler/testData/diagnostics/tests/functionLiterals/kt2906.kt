//KT-2906 If function parameter/variable is invoked in closure using parenthesis syntax, in IDEA it is not highlighted as captured in closure

package bug

public fun foo1(bar: () -> Unit) {
    run {
        bar() // ERROR: not highlighted as "captured in closure"
    }
}

public fun foo2(bar: () -> Unit) {
    run {
        bar.invoke() // CORRECT: highlighted as "captured in closure"
    }
}

fun main() {
    foo1 { println ("foo1")} // prints "foo1"
    foo2 { println ("foo2")} // prints "foo2"
}

fun println(s: String) = s

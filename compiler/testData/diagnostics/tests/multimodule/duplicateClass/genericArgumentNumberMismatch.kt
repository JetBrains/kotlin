// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// MODULE: m1
// FILE: a.kt

package p

public class A<X, Y>
public class M1 {
    public val a: A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!> = A<Int, Int>()
}

// MODULE: m2
// FILE: b.kt

package p

public class A<X, Y>

public fun foo(a: A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><Int><!>) {
}

// MODULE: m3(m1, m2)
// FILE: b.kt

import p.*

fun test() {
    foo(M1().<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>)
    foo(1) // error type on the declaration site
}
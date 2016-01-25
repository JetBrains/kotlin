// !DIAGNOSTICS: -USELESS_CAST -UNUSED_PARAMETER -UNUSED_VARIABLE

// FILE: j/Base.java
package j;
public interface Base {
    void foo();
}

// FILE: j/Impl.java
package j;

/* package */ abstract class Impl implements Base {
    public void foo() {}
}

// FILE: j/Derived1.java
package j;

public class Derived1 extends Impl {}

// FILE: j/Derived2.java
package j;

public class Derived2 extends Impl {}

// FILE: k/Client.kt
package k

import j.*

val d1 = Derived1()
val d2 = Derived2()

fun <T> select(x1: T, x2: T) = x1
fun <T> selectn(vararg xx: T) = xx[0]
fun <T : Base> foo(x: T) = x.foo()
fun <T> listOf2(x1: T, x2: T): List<T> = null!!
fun <T> arrayOf2(x1: T, x2: T): Array<T> = null!!

fun test() {
    val test1: Base = if (true) d1 else d2

    val test2 = <!INACCESSIBLE_TYPE!>if (true) d1 else d2<!>

    val test3 = <!INACCESSIBLE_TYPE!>when {
        true -> d1
        else -> d2
    }<!>

    val test4: Base = when {
        true -> d1
        else -> d2
    }

    val test5 = <!INACCESSIBLE_TYPE!>select(d1, d2)<!>

    val test6 = select<Base>(d1, d2)

    val test7 = select(d1 as Base, d2)

    val test8 = <!INACCESSIBLE_TYPE!>selectn(d1, d2)<!>

    val test9 = selectn<Base>(d1, d2)

    val test10 = <!INACCESSIBLE_TYPE!>listOf2(d1, d2)<!>

    val test11: List<Base> = <!INACCESSIBLE_TYPE!>listOf2(d1, d2)<!>
    // NB Inferred type is List<Impl> because List is covariant.

    val test12 = listOf2<Base>(d1, d2)

    val test13 = <!INACCESSIBLE_TYPE!>arrayOf2(d1, d2)<!>

    val test14: Array<Base> = arrayOf2(d1, d2)
    // NB Inferred type is Array<Base> because Array is invariant.

    val test15 = arrayOf2<Base>(d1, d2)

    for (test16 in <!INACCESSIBLE_TYPE!>listOf2(d1, d2)<!>) {}
}

fun testOkInJava() {
    // The following is Ok in Java, but is an error in Kotlin.
    // TODO do not generate unneeded CHECKCASTs.
    // TODO do not report INACCESSIBLE_TYPE for corresponding cases.
    <!INACCESSIBLE_TYPE!>select(d1, d2)<!>
    foo(<!INACCESSIBLE_TYPE!>select(d1, d2)<!>)
}

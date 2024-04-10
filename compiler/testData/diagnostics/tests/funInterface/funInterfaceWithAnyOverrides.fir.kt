// DIAGNOSTICS: -EXPOSED_SUPER_INTERFACE

// FILE: J.java
interface J1 { boolean equals(Object o); }
interface J2 { int hashCode(); }
interface J3 { String toString(); }

interface KJ1 extends K1 {}
interface KJ2 extends K2 {}
interface KJ3 extends K3 {}
interface KJ4 extends K4 {}
interface KJ5 extends K5 {}
interface KJ6 extends K6 {}
interface KJ7 extends K7 {}

// FILE: test.kt
fun interface K1 { override fun equals(other: Any?): Boolean }
fun interface K2 { override fun hashCode(): Int }
fun interface K3 { fun getClass(): Class<*> }
fun interface K4 { fun wait() }
fun interface K5 { fun notify() }
fun interface K6 { fun notifyAll() }
fun interface K7 { override fun toString(): String }

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface JK1 : J1
<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface JK2 : J2
<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface JK3 : J3

fun interface KJK1 : KJ1
fun interface KJK2 : KJ2
fun interface KJK3 : KJ3
fun interface KJK4 : KJ4
fun interface KJK5 : KJ5
fun interface KJK6 : KJ6
fun interface KJK7 : KJ7

fun test() {
    <!INTERFACE_AS_FUNCTION!>J1<!> { TODO() }
    <!INTERFACE_AS_FUNCTION!>J2<!> { TODO() }
    <!INTERFACE_AS_FUNCTION!>J3<!> { TODO() }

    KJ1 { TODO() }
    KJ2 { TODO() }
    KJ3 { TODO() }
    KJ4 { TODO() }
    KJ5 { TODO() }
    KJ6 { TODO() }
    KJ7 { TODO() }

    K1 { TODO() }
    K2 { TODO() }
    K3 { TODO() }
    K4 { TODO() }
    K5 { TODO() }
    K6 { TODO() }
    K7 { TODO() }

    <!INTERFACE_AS_FUNCTION!>JK1<!> { TODO() }
    <!INTERFACE_AS_FUNCTION!>JK2<!> { TODO() }
    <!INTERFACE_AS_FUNCTION!>JK3<!> { TODO() }

    KJK1 { TODO() }
    KJK2 { TODO() }
    KJK3 { TODO() }
    KJK4 { TODO() }
    KJK5 { TODO() }
    KJK6 { TODO() }
    KJK7 { TODO() }
}

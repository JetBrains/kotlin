// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE

// FILE: 2.kt
package b

import a.A

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
<!HIDDEN!>@kotlin.internal.HidesMembers<!>
fun A.forEach(i: Int) = i

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
<!HIDDEN!>@kotlin.internal.HidesMembers<!>
fun A.forEach(s: String) {}


// FILE: 1.kt
package a

import b.*

class A {
    fun forEach() = this
    fun forEach(i: Int) = this
    fun forEach(i: String) = this
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
<!HIDDEN!>@kotlin.internal.HidesMembers<!>
fun A.forEach() = ""

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
<!HIDDEN!>@kotlin.internal.HidesMembers<!>
fun A.forEach(s: String) {}

fun test(a: A) {
    a.forEach() checkType { _<String>() }

    a.forEach(1) checkType { _<Int>() }

    a.forEach("")

    with(a) {
        forEach() checkType { _<String>() }

        forEach(1) checkType { _<Int>() }

        forEach("")
    }
}


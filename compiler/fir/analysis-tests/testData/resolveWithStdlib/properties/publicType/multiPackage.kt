// FILE: a/Test.kt

package a

private val thing = mutableListOf("x", "y")
    public get(): List<String>

// FILE: b/Fest.kt

package b

private val thing = 4
    public get(): Number

// FILE: c/Main.kt

package c

import a.thing
import b.thing

fun main() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(<!OVERLOAD_RESOLUTION_AMBIGUITY!>thing<!>.<!UNRESOLVED_REFERENCE!>size<!>)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(<!OVERLOAD_RESOLUTION_AMBIGUITY!>thing<!> <!OVERLOAD_RESOLUTION_AMBIGUITY!>+<!> 1)
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(<!OVERLOAD_RESOLUTION_AMBIGUITY!>thing<!>)
    println(<!OVERLOAD_RESOLUTION_AMBIGUITY!>thing<!>.toString())
}

open class C1 {
    private open val number = 4
        public get(): Number
}

class C2 : C1() {
    private override val number = 3
        public get(): Number
}

fun lol() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(C2().number <!UNRESOLVED_REFERENCE!>*<!> 2)
}

<!REDECLARATION!>val testA = 10<!>
<!REDECLARATION!>val testA = "test"<!>

fun test() {
    println(testA)
}

fun fest() {
    val festA = 10
    val festA = "test"
    println(festA)
}

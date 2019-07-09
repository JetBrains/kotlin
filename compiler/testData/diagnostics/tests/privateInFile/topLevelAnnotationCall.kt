// FILE: 1.kt
package pp

private annotation class A(val s: String)
private const val foo = "O"

@A(foo)
fun f1() {}

@A(foo)
val p1 = ""

@A(foo)
class C1


// FILE: 2.kt
package pp

@<!INVISIBLE_MEMBER, INVISIBLE_REFERENCE!>A<!>(<!INVISIBLE_MEMBER!>foo<!>)
fun f2() {}

@<!INVISIBLE_MEMBER, INVISIBLE_REFERENCE!>A<!>(<!INVISIBLE_MEMBER!>foo<!>)
val p2 = ""

@<!INVISIBLE_MEMBER, INVISIBLE_REFERENCE!>A<!>(<!INVISIBLE_MEMBER!>foo<!>)
class C2

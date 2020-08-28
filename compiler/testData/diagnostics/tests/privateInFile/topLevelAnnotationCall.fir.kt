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

<!HIDDEN!>@A(<!HIDDEN!>foo<!>)<!>
fun f2() {}

<!HIDDEN!>@A(<!HIDDEN!>foo<!>)<!>
val p2 = ""

<!HIDDEN!>@A(<!HIDDEN!>foo<!>)<!>
class C2

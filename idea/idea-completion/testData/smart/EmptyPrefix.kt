open class Foo
class Bar : Foo()

val foo = Foo()
val bar = Bar()
val o : Any = ""

fun f(p1 : Foo, p2 : Bar, p3 : String, p4 : Foo?) {
    var a : Foo
    a = <caret>
}

fun f1() : Foo{}
fun f2() : Bar{}
fun f3() : String{}

// EXIST: foo
// EXIST: bar
// ABSENT: o
// EXIST: p1
// EXIST: p2
// ABSENT: p3
// ABSENT: { lookupString:"p4", itemText:"p4" }
// EXIST: { lookupString:"p4", itemText:"!! p4" }
// EXIST: { lookupString:"p4", itemText:"?: p4" }
// EXIST: f1
// EXIST: f2
// ABSENT: f3
// EXIST: { lookupString:"Foo", itemText:"Foo", tailText: "() (<root>)" }

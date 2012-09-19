// "Migrate tuples in project: e.g., #(,) and #(,,) will be replaced by Pair and Triple" "true"
// ERROR: <html>Tuples are not supported.<br/>Use data classes instead. For example:<br/><b>data class</b> FourThings(<b>val</b> a: A, <b>val</b> b: B, <b>val</b> c: C, <b>val</b> d: D)</html>
// ERROR: <html>Tuples are not supported.<br/>Use data classes instead. For example:<br/><b>data class</b> FourThings(<b>val</b> a: A, <b>val</b> b: B, <b>val</b> c: C, <b>val</b> d: D)</html>

fun foo2() : <caret>#(Int, Int) {
    return #(1, 1)
}

fun test2() {
    foo2()._1
    foo2()._2
}

fun foo3() : #(Int, Int, List<String>) {
    return #(1, 1, arrayList(""))
}

fun test3() {
    foo3()._1
    foo3()._2
    foo3()._3
}

fun foo0(): #() {
    return #()
}

fun foo4(): #(Int, Int, Int, Int) {
    return #(1, 2, 3, 4)
}

fun test4() {
    foo4()._1
    foo4()._2
    foo4()._3
    foo4()._4
}

fun fooRec() : #(Int, #(Int, String), List<#(Int, Int)>) {
    #(1, #(1, ""))
    throw Exception()
}

fun foo1(): #(Int) {
    return #(1)
}

class Fake(val _1: Int, val _2: String)

fun testFake(f: Fake) {
    f._1
    f._2
}
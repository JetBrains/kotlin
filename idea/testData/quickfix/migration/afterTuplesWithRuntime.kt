// "Migrate tuples in project: e.g., #(,) and #(,,) will be replaced by Pair and Triple" "true"
// ERROR: Tuples are not supported. Press Alt+Enter to replace tuples with library classes
// ERROR: Tuples are not supported. Press Alt+Enter to replace tuples with library classes

fun foo2() : Pair<Int, Int> {
    return Pair(1, 1)
}

fun test2() {
    foo2().first
    foo2().second
}

fun foo3() : Triple<Int, Int, List<String>> {
    return Triple(1, 1, arrayList(""))
}

fun test3() {
    foo3().first
    foo3().second
    foo3().third
}

fun foo0(): Unit {
    return Unit.VALUE
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

fun fooRec() : Triple<Int, Pair<Int, String>, List<Pair<Int, Int>>> {
    Pair(1, Pair(1, ""))
    throw Exception()
}

fun foo1(): Int {
    return 1
}

class Fake(val _1: Int, val _2: String)

fun testFake(f: Fake) {
    f._1
    f._2
}
// WITH_STDLIB

class C {
    var x: Int = 0
}

fun test(с: C?, a: Any) {
    с?.x = if (a is String) 0 else throw Exception();
    a.<!UNRESOLVED_REFERENCE!>toUpperCase<!>()
}


fun main(args: Array<String>) {
    test(null, 1)
}

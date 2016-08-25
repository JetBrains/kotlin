class Ref(var x: Int)

fun test1() {
    var x = 0
    x = 1
    x = x + 1
}

fun test2(r: Ref) {
    r.x = 0
}
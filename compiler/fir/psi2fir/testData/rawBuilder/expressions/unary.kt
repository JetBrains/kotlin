fun test() {
    var x = 0
    val x1 = x++
    val x2 = ++x
    val x3 = --x
    val x4 = x--
    if (!(x == 0)) {
        println("000")
    }
}

class X(val i: Int)

fun test2(x: X) {
    val x1 = x.i++
    val x2 = ++x.i
}

fun test3(arr: Array<Int>) {
    val x1 = arr[0]++
    val x2 = ++arr[1]
}

class Y(val arr: Array<Int>)

fun test4(y: Y) {
    val x1 = y.arr[0]++
    val x2 = ++y.arr[1]
}
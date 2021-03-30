// FIR_IGNORE
// WITH_RUNTIME
fun test() {
//      Int Int
//      │   │
    var x = 0
//           var test.x: Int
//      Int  │fun (Int).inc(): Int
//      │    ││
    val x1 = x++
//           fun (Int).inc(): Int
//      Int  │ var test.x: Int
//      │    │ │
    val x2 = ++x
//           fun (Int).dec(): Int
//      Int  │ var test.x: Int
//      │    │ │
    val x3 = --x
//           var test.x: Int
//      Int  │fun (Int).dec(): Int
//      │    ││
    val x4 = x--
//  Unit
//  │   fun (Boolean).not(): Boolean
//  │   │ var test.x: Int
//  │   │ │ EQ operator call
//  │   │ │ │  Int
//  │   │ │ │  │
    if (!(x == 0)) {
//      fun io/println(Any?): Unit
//      │
        println("000")
    }
}

class X(val i: Int)

fun test2(x: X) {
//           test2.x: X
//           │ val (X).i: Int
//      Int  │ │fun (Int).inc(): Int
//      │    │ ││
    val x1 = x.i++
//           fun (Int).inc(): Int
//           │ test2.x: X
//      Int  │ │ val (X).i: Int
//      │    │ │ │
    val x2 = ++x.i
}

fun test3(arr: Array<Int>) {
//           test3.arr: Array<Int>
//           │   Int
//      Int  │   │ fun (Int).inc(): Int
//      │    │   │ │
    val x1 = arr[0]++
//           fun (Int).inc(): Int
//           │ test3.arr: Array<Int>
//      Int  │ │   Int
//      │    │ │   │
    val x2 = ++arr[1]
}

class Y(val arr: Array<Int>)

fun test4(y: Y) {
//           test4.y: Y
//           │ val (Y).arr: Array<Int>
//           │ │   Int
//      Int  │ │   │ fun (Int).inc(): Int
//      │    │ │   │ │
    val x1 = y.arr[0]++
//           fun (Int).inc(): Int
//           │ test4.y: Y
//           │ │ val (Y).arr: Array<Int>
//      Int  │ │ │   Int
//      │    │ │ │   │
    val x2 = ++y.arr[1]
}

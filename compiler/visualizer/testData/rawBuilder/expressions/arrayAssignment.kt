fun test() {
//      IntArray
//      │   fun intArrayOf(vararg Int): IntArray
//      │   │          Int
//      │   │          │  Int
//      │   │          │  │  Int
//      │   │          │  │  │
    val x = intArrayOf(1, 2, 3)
//  val test.x: IntArray
//  │ Int  Int
//  │ │    │
    x[1] = 0
}

//        Int
//        │ Int
//        │ │
fun foo() = 1

fun test2() {
//  fun intArrayOf(vararg Int): IntArray
//  │          Int
//  │          │  Int
//  │          │  │  Int
//  │          │  │  │  fun foo(): Int
//  │          │  │  │  │        Int
//  │          │  │  │  │        │
    intArrayOf(1, 2, 3)[foo()] = 1
}

//  Int Int
//  │   │
val p = 0
//        Int
//        │ Int
//        │ │
fun foo() = 1

class Wrapper(val v: IntArray)

//                                Int
//                                │ test.a: IntArray
//                                │ │ Int
//                                │ │ │  fun (Int).plus(Int): Int
//                                │ │ │  │ test.a: IntArray
//                                │ │ │  │ │ val p: Int
//                                │ │ │  │ │ │  fun (Int).plus(Int): Int
//                                │ │ │  │ │ │  │ test.a: IntArray
//                                │ │ │  │ │ │  │ │ fun foo(): Int
//                                │ │ │  │ │ │  │ │ │      fun (Int).plus(Int): Int
//                                │ │ │  │ │ │  │ │ │      │ test.w: Wrapper
//                                │ │ │  │ │ │  │ │ │      │ │ val (Wrapper).v: IntArray
//                                │ │ │  │ │ │  │ │ │      │ │ │ Int
//                                │ │ │  │ │ │  │ │ │      │ │ │ │
fun test(a: IntArray, w: Wrapper) = a[0] + a[p] + a[foo()] + w.v[0]

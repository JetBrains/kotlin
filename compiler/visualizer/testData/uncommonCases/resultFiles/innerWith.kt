package p

class A {
//      Int     Int
//      │       │
    val aProp = 10
}

class B {
//      Int     Int
//      │       │
    val bProp = 1
}

fun foo(a: Int, b: Int): Int {
//  fun <T, R> with(A, A.() -> Int): Int
//  │    constructor A()
//  │    │    with@0
//  │    │    │
    with(A()) {
//      this@0
//      val (A).aProp: Int
//      │
        aProp

//      fun <T, R> with(B, B.() -> Int): Int
//      │    constructor B()
//      │    │    with@1
//      │    │    │
        with(B()) {
//          this@0
//          val (A).aProp: Int
//          │
            aProp
//          this@1
//          val (B).bProp: Int
//          │
            bProp
//          this@0
//          val (A).aProp: Int
//          │
            aProp
        }
    }

//  fun <T, R> with(A, A.() -> Int): Int
//  │    constructor A()
//  │    │    with@0
//  │    │    │
    with(A()) {
//      this@0
//      val (A).aProp: Int
//      │
        aProp

//      fun <T, R> with(B, B.() -> Int): Int
//      │    constructor B()
//      │    │    with@1
//      │    │    │
        with(B()) {
//          this@0
//          val (A).aProp: Int
//          │
            aProp
//          this@1
//          val (B).bProp: Int
//          │
            bProp
        }

//      fun <T, R> with(B, B.() -> Int): Int
//      │    constructor B()
//      │    │    with@1
//      │    │    │
        with(B()) {
//          this@0
//          val (A).aProp: Int
//          │
            aProp
//          this@1
//          val (B).bProp: Int
//          │
            bProp
        }
    }
//         foo.a: Int
//         │
    return a
}

package p

class A {
//      Int     Int
//      │       │
    val aProp = 10
    fun call() {}
}

class B {
//      Int     Int
//      │       │
    val bProp = 1
}

fun foo(a: Int, b: Int): Int {
//  fun <T, R> with<A, Int>(T, T.() -> R): R
//  │    constructor A()
//  │    │    with@0
//  │    │    │
    with(A()) {
//      val (A).aProp: Int
//      this@0
//      │
        aProp
//      fun (A).call(): Unit
//      this@0
//      │
        call()

//      fun <T, R> with<B, Int>(T, T.() -> R): R
//      │    constructor B()
//      │    │    with@1
//      │    │    │
        with(B()) {
//          val (A).aProp: Int
//          this@0
//          │
            aProp
//          val (B).bProp: Int
//          this@1
//          │
            bProp
//          val (A).aProp: Int
//          this@0
//          │
            aProp
        }
    }

//  fun <T, R> with<A, Int>(T, T.() -> R): R
//  │    constructor A()
//  │    │    with@0
//  │    │    │
    with(A()) {
//      val (A).aProp: Int
//      this@0
//      │
        aProp

//      fun <T, R> with<B, Int>(T, T.() -> R): R
//      │    constructor B()
//      │    │    with@1
//      │    │    │
        with(B()) {
//          val (A).aProp: Int
//          this@0
//          │
            aProp
//          val (B).bProp: Int
//          this@1
//          │
            bProp
        }

//      fun <T, R> with<B, Int>(T, T.() -> R): R
//      │    constructor B()
//      │    │    with@1
//      │    │    │
        with(B()) {
//          val (A).aProp: Int
//          this@0
//          │
            aProp
//          val (B).bProp: Int
//          this@1
//          │
            bProp
        }
    }
//         foo.a: Int
//         │
    return a
}

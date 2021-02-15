class A(val a: Int) {
    override fun equals(other: Any?): Boolean {
//      Unit
//      │   A.equals.other: Any?
//      │   │                   Boolean
//      │   │                   │
        if (other !is A) return false
//                  val (A).a: Int
//                  │ fun (Any).equals(Any?): Boolean
//                  │ │  A.equals.other: Any?
//                  │ │  │     val (A).a: Int
//                  │ │  │     │
        return this.a == other.a
    }
}

open class B(val b: Int) {
    override fun equals(other: Any?): Boolean {
//      Unit
//      │   B.equals.other: Any?
//      │   │                   Boolean
//      │   │                   │
        if (other !is B) return false
//                  val (B).b: Int
//                  │ fun (Any).equals(Any?): Boolean
//                  │ │  B.equals.other: Any?
//                  │ │  │     val (B).b: Int
//                  │ │  │     │
        return this.b == other.b
    }
}

//               constructor B(Int)
//               │ C.<init>.c: Int
//               │ │
class C(c: Int): B(c) {}

//             constructor A(Int)
//             │     fun (A).equals(Any?): Boolean
//             │     │  constructor A(Int)
//  Boolean    │ Int │  │ Int
//  │          │ │   │  │ │
val areEqual = A(10) == A(11)
//              constructor C(Int)
//              │     fun (B).equals(Any?): Boolean
//              │     │  constructor C(Int)
//  Boolean     │ Int │  │ Int
//  │           │ │   │  │ │
val areEqual2 = C(10) == C(11)
//              constructor A(Int)
//              │     fun (A).equals(Any?): Boolean
//              │     │  constructor C(Int)
//  Boolean     │ Int │  │ Int
//  │           │ │   │  │ │
val areEqual3 = A(10) == C(11)

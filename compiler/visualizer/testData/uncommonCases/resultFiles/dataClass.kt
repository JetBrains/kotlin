// FIR_IGNORE
data class Vector(val x: Int, val y: Int) {
//                                  Vector
//                                  │ constructor Vector(Int, Int)
//                                  │ │      val (Vector).x: Int
//                                  │ │      │ fun (Int).plus(Int): Int
//                                  │ │      │ │ Vector.plus.other: Vector
//                                  │ │      │ │ │     val (Vector).x: Int
//                                  │ │      │ │ │     │  val (Vector).y: Int
//                                  │ │      │ │ │     │  │ fun (Int).plus(Int): Int
//                                  │ │      │ │ │     │  │ │ Vector.plus.other: Vector
//                                  │ │      │ │ │     │  │ │ │     val (Vector).y: Int
//                                  │ │      │ │ │     │  │ │ │     │
    fun plus(other: Vector): Vector = Vector(x + other.x, y + other.y)
}

fun main() {
//      Vector
//      │   constructor Vector(Int, Int)
//      │   │      Int
//      │   │      │  Int
//      │   │      │  │
    val a = Vector(1, 2)
//      Vector
//      │   constructor Vector(Int, Int)
//      │   │       Int
//      │   │       │  Int
//      │   │       │  │
    val b = Vector(-1, 10)

//  fun io/println(Any?): Unit
//  │             val main.a: Vector
//  │             │        val main.b: Vector
//  │             │        │ fun (Vector).toString(): String
//  │             │        │ │
    println("a = $a, b = ${b.toString()}")
//  fun io/println(Any?): Unit
//  │                  fun (String).plus(Any?): String
//  │                  │  val main.a: Vector
//  │                  │  │ fun (Vector).plus(Vector): Vector
//  │                  │  │ │ val main.b: Vector
//  │                  │  │ │ │
    println("a + b = " + (a + b))
//  fun io/println(Any?): Unit
//  │                   val main.a: Vector
//  │                   │ fun (Vector).hashCode(): Int
//  │                   │ │
    println("a hash - ${a.hashCode()}")

//                             val main.a: Vector
//                             │ fun (Vector).equals(Any?): Boolean
//  fun io/println(Any?): Unit │ │      val main.b: Vector
//  │                          │ │      │
    println("a is equal to b ${a.equals(b)}")
}

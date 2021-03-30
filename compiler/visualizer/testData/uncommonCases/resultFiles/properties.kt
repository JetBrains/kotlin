package org.jetbrains.kotlin.test

//  Int  Int
//  │    │
val p1 = 10
//  Double       Double
//  │            │
val p2: Double = 1.0
//  Float       Float
//  │           │
val p3: Float = 2.5f
//  String
//  │
val p4 = "some string"

//  Double
//  │    val p1: Int
//  │    │  fun (Int).plus(Double): Double
//  │    │  │ val p2: Double
//  │    │  │ │
val p5 = p1 + p2
//  Double
//  │    val p1: Int
//  │    │  fun (Int).times(Double): Double
//  │    │  │ val p2: Double
//  │    │  │ │  fun (Double).plus(Double): Double
//  │    │  │ │  │  val p5: Double
//  │    │  │ │  │  │  fun (Double).minus(Float): Double
//  │    │  │ │  │  │  │ val p3: Float
//  │    │  │ │  │  │  │ │
val p6 = p1 * p2 + (p5 - p3)

//  Float
//  │
val withGetter
//          val p1: Int
//          │  fun (Int).times(Float): Float
//          │  │ val p3: Float
//          │  │ │
    get() = p1 * p3

//  String
//  │
var withSetter
//          val p4: String
//          │
    get() = p4
//      String   <set-withSetter>.value: String
//      │        │
    set(value) = value

//  Boolean
//  │
val withGetter2: Boolean
    get() {
//             Boolean
//             │
        return true
    }

//  String
//  │
var withSetter2: String
    get() = "1"
//      String
//      │
    set(value) {
//      var <set-withSetter2>.field: String
//      │       <set-withSetter2>.value: String
//      │       │     fun (String).plus(Any?): String
//      │       │     │
        field = value + "!"
    }

//          String
//          │
private val privateGetter: String = "cba"
    get

//  String
//  │
var privateSetter: String = "abc"
    private set

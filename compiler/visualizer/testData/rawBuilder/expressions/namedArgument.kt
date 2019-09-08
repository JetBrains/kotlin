//                                            Boolean               Double
//                                            │                     │
fun foo(first: String = "", second: Boolean = true, third: Double = 3.1415) {}

fun test() {
//  fun foo(String = ..., Boolean = ..., Double = ...): Unit
//  │
    foo()
//  fun foo(String = ..., Boolean = ..., Double = ...): Unit
//  │            Boolean
//  │            │      Double
//  │            │      │
    foo("Alpha", false, 2.71)
//  fun foo(String = ..., Boolean = ..., Double = ...): Unit
//  │                             Boolean
//  │                             │
    foo(first = "Hello", second = true)
//  fun foo(String = ..., Boolean = ..., Double = ...): Unit
//  │           fun (Double).unaryMinus(): Double
//  │           │Double
//  │           ││
    foo(third = -1.0, first = "123")
//  fun foo(String = ..., Boolean = ..., Double = ...): Unit
//  │
    foo(= "")
}

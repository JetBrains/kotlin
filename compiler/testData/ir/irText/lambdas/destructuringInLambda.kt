data class A(val x: Int, val y: Int)

var fn: (A) -> Int = { (_, y) -> 42 + y }

package lib

expect fun foo(x: Int, y: String = "OK")

expect class C(x: Int, y: String = "OK")

expect annotation class Anno1(val x: Int, val y: String = "OK")

expect annotation class Anno2(val x: Int, val y: String = "OK")

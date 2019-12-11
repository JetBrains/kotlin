annotation class Ann(val x: Int, val value: String, val y: Double)

@Ann(value = "a", x = 1, y = 1.0) fun foo1() {}
@Ann(2, "b", 2.0) fun foo2() {}
@Ann(3, "c", y = 2.0) fun foo3() {}

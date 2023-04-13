val test1 : (String) -> String = { it }
val test2 : Any.(Any) -> Any = { it.hashCode() }
val test3 = { i: Int, j: Int -> }
val test4 = fun (i: Int, j: Int) {}

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

val test1 : (String) -> String = { it }
val test2 : Any.(Any) -> Any = { it.hashCode() }
val test3 = { i: Int, j: Int -> }
val test4 = fun (i: Int, j: Int) {}

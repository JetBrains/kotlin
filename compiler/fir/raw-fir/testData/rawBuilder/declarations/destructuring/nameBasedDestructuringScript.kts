// IGNORE_TREE_ACCESS: KT-64899
// LANGUAGE: +EnableNameBasedDestructuringShortForm

class Tuple(val first: String, val second: Int)

(val a = first, var second,) = x

val (first, b = second,) = x
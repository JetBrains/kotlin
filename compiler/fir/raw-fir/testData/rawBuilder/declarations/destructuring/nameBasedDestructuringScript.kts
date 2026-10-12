// LANGUAGE: +EnableNameBasedDestructuringShortForm

class Tuple(val first: String, val second: Int)

(val a = first, var second,) = x

val (first, b = second,) = x
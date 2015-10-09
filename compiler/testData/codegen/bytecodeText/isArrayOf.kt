fun f(x: Any): Boolean = x is Array<*> && x.isArrayOf<String>()

// 2 INSTANCEOF
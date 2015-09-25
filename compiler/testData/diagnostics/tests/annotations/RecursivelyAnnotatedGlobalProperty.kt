// Properties can be recursively annotated
annotation class ann(val x: Int)
@ann(x) const val x: Int = 1
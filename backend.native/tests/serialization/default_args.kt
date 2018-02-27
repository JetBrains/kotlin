
inline val Int.prop get() = SomeDataClass(second = this)

data class SomeDataClass(val first: Int = 17, val second: Int = 19, val third: Int = 23)


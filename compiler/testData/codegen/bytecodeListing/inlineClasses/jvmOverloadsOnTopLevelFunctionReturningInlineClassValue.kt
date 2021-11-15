// WITH_STDLIB

inline class Z(val x: Int)

@JvmOverloads
fun testTopLevelFunction(x: Int = 0): Z = Z(x)
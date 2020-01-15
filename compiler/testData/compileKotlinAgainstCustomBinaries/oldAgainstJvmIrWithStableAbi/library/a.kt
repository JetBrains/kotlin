package lib

class Box(val value: String)

inline fun <T> get(block: () -> T): T = block()

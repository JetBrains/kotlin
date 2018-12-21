// IGNORE_BACKEND: JVM_IR
fun test(list: List<String>) {
    val result = mutableListOf<String>()
    use1 { list.forEach { result.add(it) } }
}

inline fun <T> use1(f: () -> T): T {
    return use2(f)
}

inline fun <T> use2(f: () -> T): T {
    try {
        return f()
    }
    catch (e: Exception) {
        throw e
    }
}

// 1 POP

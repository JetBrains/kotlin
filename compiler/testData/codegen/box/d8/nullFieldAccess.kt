// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

class Container {
    val d: String = "OK"
}

fun getContainer(flag: Boolean): Container? {
    return if (flag) Container() else null
}

fun box(): String {
    // This should trigger a null check before accessing field 'd'
    val container = getContainer(false)
    
    // Try to access field 'd' on potentially null container
    // This should be safe in Kotlin due to null safety, but might generate problematic bytecode
    return try {
        container?.d ?: "OK"
    } catch (e: Exception) {
        e.toString()
    }
}
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

class Container {
    val d: String = "OK"
}

// This function returns a nullable Container, but its return type is not explicitly marked as nullable
// This might cause isJvmNullable() to return false for the return type
fun getContainer(): Container {
    return if (System.currentTimeMillis() > 0) Container() else null as Container
}

// This function returns a constant-like expression that might be null
// This might cause isConstantLike to return true for an expression that can be null
fun getConstantLikeContainer(): Container? {
    val x: Any? = null
    return x as? Container
}

fun box(): String {
    // Test case 1: Safe call on a value that might be null but whose type is not explicitly marked as nullable
    val container1 = getContainer()
    val result1 = try {
        container1?.d ?: "OK"
    } catch (e: Exception) {
        e.toString()
    }
    
    // Test case 2: Safe call on a constant-like expression that might be null
    val container2 = getConstantLikeContainer()
    val result2 = try {
        container2?.d ?: "OK"
    } catch (e: Exception) {
        e.toString()
    }
    
    // Test case 3: Chained safe calls with a mix of nullable and non-nullable types
    val container3: Container? = if (System.currentTimeMillis() > 0) null else Container()
    val wrapper = object {
        val container: Container? = container3
    }
    val result3 = try {
        wrapper?.container?.d ?: "OK"
    } catch (e: Exception) {
        e.toString()
    }
    
    return "OK"
}
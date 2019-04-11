// IGNORE_BACKEND: JVM_IR
fun z() {}

fun foo() {
    try {
        z()
    } catch (e: Exception) {
        z()
    }
    
    try {
        z()
    } finally {
        z()
    }

    try {
        z()
    } catch (e: Exception) {
        z()
    } finally {
        z()
    }
}

// 0 GETSTATIC

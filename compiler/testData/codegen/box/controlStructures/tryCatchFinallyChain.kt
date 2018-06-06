// IGNORE_BACKEND: JS_IR
fun box() : String {
    try {
    } finally {
        try {
            try {
            } finally {
                try {
                } finally {
                }
            }
        } catch (e: Exception) {
            try {
            } catch (f: Exception) {
            } finally {
            }
        }
        return "OK"
    }
}

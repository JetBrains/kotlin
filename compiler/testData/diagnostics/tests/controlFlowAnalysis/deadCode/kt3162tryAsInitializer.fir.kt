//KT-3162 More precise try-finally error marking

fun foo(x: String) : String {
    val a = try {
        x
    } finally {
        try {
        } catch (e: Exception) {
        }
        return x
    }
}
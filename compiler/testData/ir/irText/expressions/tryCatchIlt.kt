// ISSUE: KT-77774

fun test(): Long {
    return try {
        42L
    } catch (e: Throwable) {
        -1
    }
}
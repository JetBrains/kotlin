//KT-2585 Code in try-finally is incorrectly marked as unreachable

fun foo() {
    try {
        throw RuntimeException()
    } catch (e: Exception) {
        return     // <- Wrong UNREACHABLE_CODE
    } finally {
        while (true);
    }
}

fun bar() {
    try {
        throw RuntimeException()
    } catch (e: Exception) {
        return     // <- Wrong UNREACHABLE_CODE
    } finally {
        while (cond());
    }
}

fun cond() = true
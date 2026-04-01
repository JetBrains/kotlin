// ISSUE: KT-50028
// IGNORE_BACKEND: JKLIB

fun test_1(): String {
    return when {
        else -> {
            return ""
        }
    }
}

fun test_2(b: Boolean): Boolean {
    return if (b) {
        true
    } else {
        throw NotImplementedError()
    }
}

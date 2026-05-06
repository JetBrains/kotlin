// ISSUE: KT-50028

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

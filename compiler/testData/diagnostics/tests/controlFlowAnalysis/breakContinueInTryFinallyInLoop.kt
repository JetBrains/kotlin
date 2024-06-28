// ISSUE: KT-51759

fun testBreak(b: Boolean, s: String?) {
    while (b) {
        val x: String?
        try {
            x = s ?: break
        } finally {
        }
        x!!.length
    }
}

fun testContinue(b: Boolean, s: String?) {
    while (b) {
        val x: String?
        try {
            x = s ?: continue
        } finally {
        }
        x!!.length
    }
}

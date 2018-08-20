// !LANGUAGE: +VariableDeclarationInWhenSubject
// !DIAGNOSTICS: -UNUSED_VARIABLE


fun testJumpOutInElvis(x: Int?) {
    loop@ while (true) {
        val y = when (val z = x ?: break@loop) {
            0 -> "0"
            else -> "not 0"
        }

        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }

    x<!UNSAFE_CALL!>.<!>inc()
}

fun testJumpOutInElvisLikeIf(x: Int?) {
    loop@ while (true) {
        val y = when (val z = if (x == null) break@loop else <!DEBUG_INFO_SMARTCAST!>x<!>) {
            0 -> "0"
            else -> "not 0"
        }
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }

    x<!UNSAFE_CALL!>.<!>inc()
}


fun getBoolean() = true

fun testJumpOutInIf(x: Int?) {
    loop@ while (true) {
        val y = when (val z = if (getBoolean()) { x!!; break@loop } else x) {
            0 -> "0"
            else -> "not 0"
        }
        x<!UNSAFE_CALL!>.<!>inc()
    }

    x<!UNSAFE_CALL!>.<!>inc() // Actually, safe, but it's OK if it's error
}
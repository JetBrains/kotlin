// !SANITIZE_PARENTHESES
// IGNORE_BACKEND: JS, JS_IR

// Sanitization is needed here because of an ASM bug: https://gitlab.ow2.org/asm/asm/issues/317868
// As soon as that bug is fixed and we've updated to the new version of ASM, this test will start to pass without sanitization.
// At that point, we should remove the -Xsanitize-parentheses compiler argument.
// Also don't forget to disable this test on Android where parentheses are not allowed in names

class `()` {
    fun `()`(): String {
        fun foo(): String {
            return bar { baz() }
        }
        return foo()
    }

    fun baz() = "OK"
}

fun bar(p: () -> String) = p()

fun box(): String {
    return `()`().`()`()
}

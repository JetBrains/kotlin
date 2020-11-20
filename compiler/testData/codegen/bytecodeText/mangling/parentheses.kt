// !SANITIZE_PARENTHESES

class `(X)` {
    fun `(Y)`(): String {
        fun foo(): String {
            return bar { baz() }
        }
        return foo()
    }

    fun baz() = "OK"
}

fun bar(p: () -> String) = p()

fun box(): String {
    return `(X)`().`(Y)`()
}

// JVM_TEMPLATES
// One instance of each is in kotlin.Metadata.d2
// 1 \(X\)
// 1 \(Y\)

// JVM_IR_TEMPLATES
// 4 this\$0

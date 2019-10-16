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

// One instance of each is in kotlin.Metadata.d2
// 1 \(X\)

// JVM_TEMPLATES
// 1 \(Y\)

// JVM_IR_TEMPLATES
// 5 \(Y\)
// 4 \$this\$\(Y\)

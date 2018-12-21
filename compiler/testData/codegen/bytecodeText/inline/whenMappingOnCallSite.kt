// IGNORE_BACKEND: JVM_IR
package test

enum class X {
    A,
    B
}

inline fun test(x: X, s: (X) -> String): String {
    return s(x)
}


fun box(): String {
    return test(X.A) {
        when(it) {
            X.A-> "O"
            X.B-> "K"
        }
    } + test(X.B) {
        when(it) {
            X.A-> "O"
            X.B-> "K"
        }
    }
}

// no additional mappings cause when in inline lambda (same module)
// 1 class test/.*\$WhenMappings
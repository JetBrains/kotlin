// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

suspend fun foo() = 1

suspend fun bar(
        x: Int = 2 + foo(),
        y: suspend () -> Int = { foo() },
        z: () -> Int = { foo() },
        w: Int = myInline { foo() },
        v: Any? = object {
            fun x() = foo()
            suspend fun y() = foo()
        }
) {}

inline fun myInline(x: () -> Unit) = 1

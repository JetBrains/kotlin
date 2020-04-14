// FILE: test.kt
fun box() {
    t { (`a b`, `b$c`, `c-d`, `b$$c--d`, `a()§&*&^@あ化`) -> }
}

private fun t(block: (Arr) -> Unit) {
    block(Arr())
}

private data class Arr(
    val a1: Int = 0,
    val a2: Int = 0,
    val a3: Int = 0,
    val a4: Int = 0,
    val a5: Int = 0
)

// IGNORE_BACKEND: JVM_IR

// LOCAL VARIABLES
// TestKt:3:
// TestKt:7: LV:block:TestKt$box$1
// Arr:11: F:a1:int, F:a2:int, F:a3:int, F:a4:int, F:a5:int
// Arr:12: F:a1:int, F:a2:int, F:a3:int, F:a4:int, F:a5:int
// Arr:13: F:a1:int, F:a2:int, F:a3:int, F:a4:int, F:a5:int
// Arr:14: F:a1:int, F:a2:int, F:a3:int, F:a4:int, F:a5:int
// Arr:15: F:a1:int, F:a2:int, F:a3:int, F:a4:int, F:a5:int
// Arr:10: F:a1:int, F:a2:int, F:a3:int, F:a4:int, F:a5:int, LV:a1:int, LV:a2:int, LV:a3:int, LV:a4:int, LV:a5:int
// Arr:15: F:a1:int, F:a2:int, F:a3:int, F:a4:int, F:a5:int
// TestKt:7: LV:block:TestKt$box$1
// TestKt$box$1:3: F:INSTANCE:TestKt$box$1, F:arity:int, LV:$dstr$a_u20b$b_u24c$c_u2dd$b_u24_u24c_u2d_u2dd$a_u28_u29§_u26_u2a_u26_u5e_u40あ化:Arr
// TestKt$box$1.invoke(java.lang.Object)+8: F:INSTANCE:TestKt$box$1, F:arity:int
// TestKt$box$1.invoke(java.lang.Object)+11: F:INSTANCE:TestKt$box$1, F:arity:int
// TestKt:7: LV:block:TestKt$box$1
// TestKt:8: LV:block:TestKt$box$1
// TestKt:4:
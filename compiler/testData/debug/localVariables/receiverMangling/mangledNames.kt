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
// TestKt:7: block:TestKt$box$1
// Arr:11:
// Arr:12:
// Arr:13:
// Arr:14:
// Arr:15:
// Arr:10: a1:int, a2:int, a3:int, a4:int, a5:int
// Arr:15:
// TestKt:7: block:TestKt$box$1
// TestKt$box$1:3: $dstr$a_u20b$b_u24c$c_u2dd$b_u24_u24c_u2d_u2dd$a_u28_u29§_u26_u2a_u26_u5e_u40あ化:Arr
// TestKt$box$1.invoke(java.lang.Object)+8:
// TestKt$box$1.invoke(java.lang.Object)+11:
// TestKt:7: block:TestKt$box$1
// TestKt:8: block:TestKt$box$1
// TestKt:4:
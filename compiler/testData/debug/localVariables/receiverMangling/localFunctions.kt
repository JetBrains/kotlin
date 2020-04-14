// FILE: test.kt
fun foo() {
    fun a() {}
    fun a2() {}
    fun a2(a: Int) {}
    fun `b c`() {}
    fun `c$d`() {}

    a()
    a2()
    a2(42)
    `b c`()
    `c$d`()
}

fun box() {
    foo()
}

// IGNORE_BACKEND: JVM_IR

// LOCAL VARIABLES
// TestKt:17:
// TestKt:3:
// TestKt:4: $fun$a$1:TestKt$foo$1
// TestKt:5: $fun$a$1:TestKt$foo$1, $fun$a2$2:TestKt$foo$2
// TestKt:6: $fun$a$1:TestKt$foo$1, $fun$a2$2:TestKt$foo$2, $fun$a2$3:TestKt$foo$3
// TestKt:7: $fun$a$1:TestKt$foo$1, $fun$a2$2:TestKt$foo$2, $fun$a2$3:TestKt$foo$3, $fun$b_u20c$4:TestKt$foo$4
// TestKt:9: $fun$a$1:TestKt$foo$1, $fun$a2$2:TestKt$foo$2, $fun$a2$3:TestKt$foo$3, $fun$b_u20c$4:TestKt$foo$4, $fun$c_u24d$5:TestKt$foo$5
// TestKt$foo$1:3:
// TestKt:10: $fun$a$1:TestKt$foo$1, $fun$a2$2:TestKt$foo$2, $fun$a2$3:TestKt$foo$3, $fun$b_u20c$4:TestKt$foo$4, $fun$c_u24d$5:TestKt$foo$5
// TestKt$foo$2:4:
// TestKt:11: $fun$a$1:TestKt$foo$1, $fun$a2$2:TestKt$foo$2, $fun$a2$3:TestKt$foo$3, $fun$b_u20c$4:TestKt$foo$4, $fun$c_u24d$5:TestKt$foo$5
// TestKt$foo$3:5: a:int
// TestKt:12: $fun$a$1:TestKt$foo$1, $fun$a2$2:TestKt$foo$2, $fun$a2$3:TestKt$foo$3, $fun$b_u20c$4:TestKt$foo$4, $fun$c_u24d$5:TestKt$foo$5
// TestKt$foo$4:6:
// TestKt:13: $fun$a$1:TestKt$foo$1, $fun$a2$2:TestKt$foo$2, $fun$a2$3:TestKt$foo$3, $fun$b_u20c$4:TestKt$foo$4, $fun$c_u24d$5:TestKt$foo$5
// TestKt$foo$5:7:
// TestKt:14: $fun$a$1:TestKt$foo$1, $fun$a2$2:TestKt$foo$2, $fun$a2$3:TestKt$foo$3, $fun$b_u20c$4:TestKt$foo$4, $fun$c_u24d$5:TestKt$foo$5
// TestKt:18:
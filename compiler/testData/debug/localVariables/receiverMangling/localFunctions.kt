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
// TestKt:4: LV:$fun$a$1:TestKt$foo$1
// TestKt:5: LV:$fun$a$1:TestKt$foo$1, LV:$fun$a2$2:TestKt$foo$2
// TestKt:6: LV:$fun$a$1:TestKt$foo$1, LV:$fun$a2$2:TestKt$foo$2, LV:$fun$a2$3:TestKt$foo$3
// TestKt:7: LV:$fun$a$1:TestKt$foo$1, LV:$fun$a2$2:TestKt$foo$2, LV:$fun$a2$3:TestKt$foo$3, LV:$fun$b_u20c$4:TestKt$foo$4
// TestKt:9: LV:$fun$a$1:TestKt$foo$1, LV:$fun$a2$2:TestKt$foo$2, LV:$fun$a2$3:TestKt$foo$3, LV:$fun$b_u20c$4:TestKt$foo$4, LV:$fun$c_u24d$5:TestKt$foo$5
// TestKt$foo$1:3: F:INSTANCE:TestKt$foo$1, F:arity:int
// TestKt:10: LV:$fun$a$1:TestKt$foo$1, LV:$fun$a2$2:TestKt$foo$2, LV:$fun$a2$3:TestKt$foo$3, LV:$fun$b_u20c$4:TestKt$foo$4, LV:$fun$c_u24d$5:TestKt$foo$5
// TestKt$foo$2:4: F:INSTANCE:TestKt$foo$2, F:arity:int
// TestKt:11: LV:$fun$a$1:TestKt$foo$1, LV:$fun$a2$2:TestKt$foo$2, LV:$fun$a2$3:TestKt$foo$3, LV:$fun$b_u20c$4:TestKt$foo$4, LV:$fun$c_u24d$5:TestKt$foo$5
// TestKt$foo$3:5: F:INSTANCE:TestKt$foo$3, F:arity:int, LV:a:int
// TestKt:12: LV:$fun$a$1:TestKt$foo$1, LV:$fun$a2$2:TestKt$foo$2, LV:$fun$a2$3:TestKt$foo$3, LV:$fun$b_u20c$4:TestKt$foo$4, LV:$fun$c_u24d$5:TestKt$foo$5
// TestKt$foo$4:6: F:INSTANCE:TestKt$foo$4, F:arity:int
// TestKt:13: LV:$fun$a$1:TestKt$foo$1, LV:$fun$a2$2:TestKt$foo$2, LV:$fun$a2$3:TestKt$foo$3, LV:$fun$b_u20c$4:TestKt$foo$4, LV:$fun$c_u24d$5:TestKt$foo$5
// TestKt$foo$5:7: F:INSTANCE:TestKt$foo$5, F:arity:int
// TestKt:14: LV:$fun$a$1:TestKt$foo$1, LV:$fun$a2$2:TestKt$foo$2, LV:$fun$a2$3:TestKt$foo$3, LV:$fun$b_u20c$4:TestKt$foo$4, LV:$fun$c_u24d$5:TestKt$foo$5
// TestKt:18:
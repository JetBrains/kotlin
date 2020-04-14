// FILE: test.kt

fun box() {
    (fun (blockArg: String.() -> Unit) =
        "OK".blockArg()) {
        println(this)
    }
}

// IGNORE_BACKEND: JVM_IR
// LOCAL VARIABLES
// TestKt:4: JDI Exception, no local variable information for method TestKt.box()
// TestKt:5: JDI Exception, no local variable information for method TestKt.box()
// TestKt$box$1:5: F:INSTANCE:TestKt$box$1, F:arity:int, LV:blockArg:TestKt$box$2
// TestKt$box$2:6: F:INSTANCE:TestKt$box$2, F:arity:int, LV:$this$invoke:java.lang.String
// TestKt$box$2:7: F:INSTANCE:TestKt$box$2, F:arity:int, LV:$this$invoke:java.lang.String
// TestKt$box$2.invoke(java.lang.Object)+8: F:INSTANCE:TestKt$box$2, F:arity:int
// TestKt$box$2.invoke(java.lang.Object)+11: F:INSTANCE:TestKt$box$2, F:arity:int
// TestKt$box$1:5: F:INSTANCE:TestKt$box$1, F:arity:int, LV:blockArg:TestKt$box$2
// TestKt$box$1.invoke(java.lang.Object)+8: F:INSTANCE:TestKt$box$1, F:arity:int
// TestKt$box$1.invoke(java.lang.Object)+11: F:INSTANCE:TestKt$box$1, F:arity:int
// TestKt:5: JDI Exception, no local variable information for method TestKt.box()
// TestKt:8: JDI Exception, no local variable information for method TestKt.box()

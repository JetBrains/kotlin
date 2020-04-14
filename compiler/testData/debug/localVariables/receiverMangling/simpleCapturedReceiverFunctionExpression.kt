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
// TestKt$box$1:5: blockArg:TestKt$box$2
// TestKt$box$2:6: $this$invoke:java.lang.String
// TestKt$box$2:7: $this$invoke:java.lang.String
// TestKt$box$2.invoke(java.lang.Object)+8:
// TestKt$box$2.invoke(java.lang.Object)+11:
// TestKt$box$1:5: blockArg:TestKt$box$2
// TestKt$box$1.invoke(java.lang.Object)+8:
// TestKt$box$1.invoke(java.lang.Object)+11:
// TestKt:5: JDI Exception, no local variable information for method TestKt.box()
// TestKt:8: JDI Exception, no local variable information for method TestKt.box()
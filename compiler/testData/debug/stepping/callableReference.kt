// FILE: test.kt
fun box() {
    var x = false
    f {
        x = true
    }
}

fun f(block: () -> Unit) {
    block()
}

// LINENUMBERS
// TestKt.box():3
// TestKt.box():4
// TestKt.f(kotlin.jvm.functions.Function0):10
// TestKt$box$1.invoke():5
// TestKt$box$1.invoke():6
// TestKt$box$1.invoke():-1
// TestKt$box$1.invoke():-1
// TestKt.f(kotlin.jvm.functions.Function0):10
// TestKt.f(kotlin.jvm.functions.Function0):11
// TestKt.box():7

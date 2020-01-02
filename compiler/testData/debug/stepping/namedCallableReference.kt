// FILE: test.kt
fun box() {
    var x = false
    f(::g)
}

fun f(block: () -> Unit) {
    block()
}

fun g() {}

// LINENUMBERS
// TestKt.box():3
// TestKt.box():4
// TestKt.f(kotlin.jvm.functions.Function0):8
// TestKt.g():11
// TestKt$box$1.invoke():4
// TestKt$box$1.invoke():-1
// TestKt$box$1.invoke():-1
// TestKt.f(kotlin.jvm.functions.Function0):8
// TestKt.f(kotlin.jvm.functions.Function0):9
// TestKt.box():5

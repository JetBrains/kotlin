// WITH_FIR_TEST_COMPILER_PLUGIN
// DUMP_IR

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: p3/foo.kt
package p3

import org.jetbrains.kotlin.fir.plugin.MyComposable

@MyComposable
fun Scaffold(bars: List<@MyComposable () -> Unit>) {
}

// MODULE: main(lib)
// FILE: main.kt
import org.jetbrains.kotlin.fir.plugin.MyComposable
import p3.Scaffold

@MyComposable
private fun TopAppBar(title: String) {
}

@MyComposable
private fun ArticleScreenContent(title: String, bottomBarContent: @MyComposable () -> Unit = { }) {
    Scaffold(listOf({ TopAppBar(title) }, bottomBarContent))
}

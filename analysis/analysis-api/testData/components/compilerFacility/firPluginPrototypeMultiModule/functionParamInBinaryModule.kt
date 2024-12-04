// WITH_FIR_TEST_COMPILER_PLUGIN
// DUMP_IR

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: p3/foo.kt
package p3

import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

@MyInlineable
fun Scaffold(topBar: @MyInlineable () -> Unit, bottomBar: @MyInlineable () -> Unit) {
}

// MODULE: main(lib)
// FILE: main.kt
import org.jetbrains.kotlin.plugin.sandbox.MyInlineable
import p3.Scaffold

@MyInlineable
private fun TopAppBar(title: String) {
}

@MyInlineable
private fun ArticleScreenContent(title: String, bottomBarContent: @MyInlineable () -> Unit = { }) {
    Scaffold(topBar = { TopAppBar(title) }, bottomBar = bottomBarContent)
}

@file:Suppress("UNUSED_PARAMETER")

package sample

expect interface <!LINE_MARKER("descr='Is subclassed by A [common-2] B [jvm]'"), LINE_MARKER("descr='Has actuals in JVM'")!>C<!> {
    fun <!LINE_MARKER("descr='Has actuals in JVM'")!>common_2_C<!>()
}

actual interface <!LINE_MARKER("descr='Is implemented by B [jvm]'"), LINE_MARKER("descr='Has declaration in common module'")!>A<!> : C {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>common_1_A<!>()
    fun common_2_A()
}

typealias A_Common_2_Alias = A
typealias B_Common_2_Alias = B
typealias C_Common_2_Alias = C

fun take_A_common_2(func: (A) -> Unit) {}
fun take_B_common_2(func: (B) -> Unit) {}
fun take_C_common_2(func: (C) -> Unit) {}

fun take_A_alias_common_2(func: (A_Common_2_Alias) -> Unit) {}
fun take_B_alias_common_2(func: (B_Common_2_Alias) -> Unit) {}
fun take_C_alias_common_2(func: (C_Common_2_Alias) -> Unit) {}
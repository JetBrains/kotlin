@file:Suppress("UNUSED_PARAMETER")

package sample

expect interface C {
    fun common_2_C()
}

actual interface A : C {
    actual fun common_1_A()
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
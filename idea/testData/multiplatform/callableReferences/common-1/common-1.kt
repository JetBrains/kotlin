@file:Suppress("UNUSED_PARAMETER")

package sample

expect interface A {
    fun common_1_A()
}

expect interface B : A {
    fun common_1_B()
}

typealias A_Common_1_Alias = A
typealias B_Common_1_Alias = B

fun take_A_common_1(func: (A) -> Unit) {}
fun take_B_common_1(func: (B) -> Unit) {}

fun take_A_alias_common_1(func: (A_Common_1_Alias) -> Unit) {}
fun take_B_alias_common_1(func: (B_Common_1_Alias) -> Unit) {}
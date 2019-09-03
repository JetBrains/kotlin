package sample

fun test_1() {
    take_A_common_1(A::common_1_A)
    take_A_alias_common_1(A::common_1_A)
}

fun test_2() {
    take_B_common_1(B::common_1_A)
    take_B_alias_common_1(B::common_1_A)
    take_B_common_1(B::common_1_B)
    take_B_alias_common_1(B::common_1_B)
}

fun test_3() {
    take_A_common_1(A_Common_1_Alias::common_1_A)
    take_A_alias_common_1(A_Common_1_Alias::common_1_A)
}

fun test_4() {
    take_B_common_1(B_Common_1_Alias::common_1_A)
    take_B_alias_common_1(B_Common_1_Alias::common_1_A)
    take_B_common_1(B_Common_1_Alias::common_1_B)
    take_B_alias_common_1(B_Common_1_Alias::common_1_B)
}
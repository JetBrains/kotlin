package sample

fun test_1() {
    take_A_common_1(A::common_1_A)
    take_A_alias_common_1(A::common_1_A)
    take_A_common_1(A::common_2_A)
    take_A_alias_common_1(A::common_2_A)
    take_A_common_1(C::common_2_C)
    take_A_alias_common_1(C::common_2_C)
}

fun test_1_alias_1() {
    take_A_common_1(A_Common_1_Alias::common_1_A)
    take_A_alias_common_1(A_Common_1_Alias::common_1_A)
    take_A_common_1(A_Common_1_Alias::common_2_A)
    take_A_alias_common_1(A_Common_1_Alias::common_2_A)
}

fun test_1_alias_2() {
    take_A_common_1(A_Common_2_Alias::common_1_A)
    take_A_alias_common_1(A_Common_2_Alias::common_1_A)
    take_A_common_1(A_Common_2_Alias::common_2_A)
    take_A_alias_common_1(A_Common_2_Alias::common_2_A)
    take_A_common_1(C_Common_2_Alias::common_2_C)
    take_A_alias_common_1(C_Common_2_Alias::common_2_C)
}

fun test_2() {
    take_A_common_2(A::common_2_A)
    take_A_alias_common_2(A::common_2_A)
    take_A_common_2(A::common_2_A)
    take_A_alias_common_2(A::common_2_A)
    take_A_common_2(C::common_2_C)
    take_A_alias_common_2(C::common_2_C)
}

fun test_2_alias_1() {
    take_A_common_2(A_Common_1_Alias::common_2_A)
    take_A_alias_common_2(A_Common_1_Alias::common_2_A)
    take_A_common_2(A_Common_1_Alias::common_2_A)
    take_A_alias_common_2(A_Common_1_Alias::common_2_A)
}

fun test_2_alias_2() {
    take_A_common_2(A_Common_2_Alias::common_2_A)
    take_A_alias_common_2(A_Common_2_Alias::common_2_A)
    take_A_common_2(A_Common_2_Alias::common_2_A)
    take_A_alias_common_2(A_Common_2_Alias::common_2_A)
    take_A_common_2(C_Common_2_Alias::common_2_C)
    take_A_alias_common_2(C_Common_2_Alias::common_2_C)
}

fun test_3() {
    take_C_common_2(C::common_2_C)
    take_C_alias_common_2(C::common_2_C)
    take_C_common_2(C_Common_2_Alias::common_2_C)
    take_C_alias_common_2(C_Common_2_Alias::common_2_C)
}

fun test_4() {
    take_B_common_1(B::common_1_A)
    take_B_common_1(B::common_2_A)
    take_B_common_2(B::common_1_A)
    take_B_common_2(B::common_2_A)

    take_B_alias_common_1(B::common_1_A)
    take_B_alias_common_1(B::common_2_A)
    take_B_alias_common_2(B::common_1_A)
    take_B_alias_common_2(B::common_2_A)
}

fun test_4_alias_1() {
    take_B_common_1(B_Common_1_Alias::common_1_A)
    take_B_common_1(B_Common_1_Alias::common_2_A)
    take_B_common_2(B_Common_1_Alias::common_1_A)
    take_B_common_2(B_Common_1_Alias::common_2_A)

    take_B_alias_common_1(B_Common_1_Alias::common_1_A)
    take_B_alias_common_1(B_Common_1_Alias::common_2_A)
    take_B_alias_common_2(B_Common_1_Alias::common_1_A)
    take_B_alias_common_2(B_Common_1_Alias::common_2_A)
}

fun test_4_alias_2() {
    take_B_common_1(B_Common_2_Alias::common_1_A)
    take_B_common_1(B_Common_2_Alias::common_2_A)
    take_B_common_2(B_Common_2_Alias::common_1_A)
    take_B_common_2(B_Common_2_Alias::common_2_A)

    take_B_alias_common_1(B_Common_2_Alias::common_1_A)
    take_B_alias_common_1(B_Common_2_Alias::common_2_A)
    take_B_alias_common_2(B_Common_2_Alias::common_1_A)
    take_B_alias_common_2(B_Common_2_Alias::common_2_A)
}
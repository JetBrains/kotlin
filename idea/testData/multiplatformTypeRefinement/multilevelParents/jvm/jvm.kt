package sample

actual interface A_Common {
    actual fun common_1_A()
    actual fun common_2_A()
    fun jvm_A()
}

interface Case_2_3 : B {
    fun jvm_Case_2_3()
}

fun test_A(x: B) {
    x.common_1_A()
    x.common_1_B()
    x.common_2_A()
    x.common_1_2_B()
    x.jvm_A()
}

fun test_A(x: Case_2_3) {
    x.common_1_A()
    x.common_1_B()
    x.common_2_A()
    x.common_1_2_B()
    x.jvm_A()
    x.jvm_Case_2_3()
}

fun test_B() {
    val x = getB()
    x.common_1_A()
    x.common_1_B()
    x.common_2_A()
    x.common_1_2_B()
    x.jvm_A()
}

fun test_case_2_3() {
    takeOutA_common_1(getOutB())
    takeOutB_common_1(getOutB())
    takeOutA_common_2(getOutB())
    takeOutB_common_2(getOutB())
    takeOutA_Common_common_2(getOutB())

    takeOutA_common_1(getOutA())
    takeOutA_common_2(getOutA())
    takeOutA_Common_common_2(getOutA())

    takeOutA_common_1(getOutA_Common())
    takeOutA_common_2(getOutA_Common())
    takeOutA_Common_common_2(getOutA_Common())
}
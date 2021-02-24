@file:Suppress("UNUSED_PARAMETER")

package sample

expect interface <!LINE_MARKER("descr='Is subclassed by B [common-2] Case_2_3'"), LINE_MARKER("descr='Has actuals in JVM'")!>A_Common<!> {
    fun <!LINE_MARKER("descr='Has actuals in JVM'")!>common_1_A<!>()
    fun <!LINE_MARKER("descr='Has actuals in JVM'")!>common_2_A<!>()
}

actual typealias <!LINE_MARKER("descr='Has declaration in common module'")!>A<!> = A_Common

actual interface <!LINE_MARKER("descr='Is implemented by Case_2_3'"), LINE_MARKER("descr='Has declaration in common module'")!>B<!> : A {
    actual fun <!LINE_MARKER("descr='Has declaration in common module'")!>common_1_B<!>()
    fun common_1_2_B()
}

fun takeOutA_common_2(t: Out<A>) {}
fun takeOutB_common_2(t: Out<B>) {}
fun takeOutA_Common_common_2(t: Out<A_Common>) {}

fun getOutA(): Out<A> = null!!
fun getOutB(): Out<B> = null!!
fun getOutA_Common(): Out<A_Common> = null!!

fun test_case_2(x: B) {
    x.common_1_A()
    x.common_1_B()
    x.common_2_A()
    x.common_1_2_B()
}

fun test_B() {
    val x = getB()
    x.common_1_A()
    x.common_1_B()
    x.common_2_A()
    x.common_1_2_B()
}
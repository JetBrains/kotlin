@file:Suppress("UNUSED_PARAMETER")

package sample

expect interface <!LINE_MARKER("descr='Is subclassed by B [common-1] B [common-2] Case_2_3'"), LINE_MARKER("descr='Has actuals in common'")!>A<!> {
    fun common_1_A()
}

expect interface <!LINE_MARKER("descr='Is subclassed by Case_2_3'"), LINE_MARKER("descr='Has actuals in common'")!>B<!> : A {
    fun <!LINE_MARKER("descr='Has actuals in common'")!>common_1_B<!>()
}

fun getB(): B = null!!

class Out<out T>(val value: T)

fun takeOutA_common_1(t: Out<A>) {}
fun takeOutB_common_1(t: Out<B>) {}
@file:Suppress("NO_ACTUAL_FOR_EXPECT")

package foo

expect interface <!LINE_MARKER("descr='Is subclassed by AImpl'"), LINE_MARKER("descr='Has actuals in JVM'")!>A<!> {
    fun <!LINE_MARKER("descr='Has actuals in JVM'"), LINE_MARKER("descr='Is overridden in foo.AImpl'")!>commonFun<!>()
}

class CommonGen<T : A> {
    val a: T get() = null!!
}

class List<out T>(val value: T)

fun getList(): List<A> = null!!
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-70894

// MODULE: common
// FILE: common.kt
interface Base {
    fun method(): String
}

expect interface Foo : Base {
    override fun method(): String
}

class DelegatedImpl(val delegate: Foo) : Base by delegate

// MODULE: platform()()(common)
// FILE: platform.kt
actual typealias Foo = BaseInterface

interface BaseInterface : Base

class BaseImpl : BaseInterface {
    override fun method(): String {
        return "OK"
    }
}

fun box(): String {
    val x = DelegatedImpl(BaseImpl())
    return x.method()
}

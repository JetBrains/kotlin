// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-70894

// MODULE: common
// FILE: common.kt
interface Base {
    fun method(): String
}

expect interface Foo : Base

interface BaseImpl: Base

class DelegatedImpl(val delegate: Foo) : Base by delegate

// MODULE: platform()()(common)
// FILE: BaseJava.java
public interface BaseJava extends BaseImpl { }

// FILE: platform.kt
actual typealias Foo = BaseJava

class BaseJavaImpl : BaseJava {
    override fun method(): String {
        return "OK"
    }
}

fun box(): String {
    val x = DelegatedImpl(BaseJavaImpl())
    return x.method()
}

// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-70894

// MODULE: common
// FILE: common.kt
expect interface Base
expect interface BaseImpl : Base
expect interface Foo : BaseImpl

class DelegatedImpl(val delegate: Foo) : Base by delegate

// MODULE: platform()()(common)
// FILE: BaseJava.java
public interface BaseJava {
    String method();
}

// FILE: BaseJavaImpl.java
public interface BaseJavaImpl extends BaseImpl {}

// FILE: platform.kt
actual typealias Base = BaseJava
actual interface BaseImpl : Base
actual typealias Foo = BaseJavaImpl

class BaseClass : BaseJavaImpl {
    override fun method(): String {
        return "OK"
    }
}

fun box(): String {
    val x = DelegatedImpl(BaseClass())
    return x.method()
}

// B
// LIBRARY_PLATFORMS: JVM
// JVM_DEFAULT_MODE: disable

// FILE: JavaInterface.java
public interface JavaInterface {
    default void javaDefault() {}
}

// FILE: B.kt
interface Generic<T> {
    fun generic(t: T): T = t
}

interface A : JavaInterface {
    fun a() {}

    val prop: Int
        get() = 1

    private fun privateFun() {}

    fun abstractFun()

    fun overriddenWithBody() {}

    fun overriddenAsAbstract() {}
}

interface B : A, Generic<String> {
    override fun overriddenWithBody() {}

    abstract override fun overriddenAsAbstract()

    fun b() {}
}

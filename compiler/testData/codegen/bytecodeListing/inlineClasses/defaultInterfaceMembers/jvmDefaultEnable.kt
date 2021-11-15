// !JVM_DEFAULT_MODE: enable
// WITH_STDLIB
// JVM_TARGET: 1.8

interface IFooBar {
    @JvmDefault fun foo() = "O"
    @JvmDefault fun bar() = "Failed"
}

interface IFooBar2 : IFooBar

inline class Test1(val k: String): IFooBar {
    override fun bar(): String = k
}

inline class Test2(val k: String): IFooBar2 {
    override fun bar(): String = k
}

// JVM_TARGET: 1.8
// FILE: javaDefaultInterfaceMember.kt
interface KFoo2 : JIFoo

interface KFooUnrelated {
    fun foo()
}

interface KFoo3 : KFoo2, KFooUnrelated {
    override fun foo() {}
}

inline class Test1(val x: Int) : JIFoo

inline class Test2(val x: Int) : KFoo2

inline class Test3(val x: Int) : KFoo3

// FILE: JIFoo.java
public interface JIFoo {
    default void foo() {}
}

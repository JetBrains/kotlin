// JVM_TARGET: 1.8
// FILE: javaDefaultInterfaceMember.kt
interface KFoo2 : JIFoo

interface KFooUnrelated {
    fun foo()
}

interface KFoo3 : KFoo2, KFooUnrelated {
    override fun foo() {}
}

interface KBar2 : JIBar

inline class TestFoo1(val x: Int) : JIFoo

inline class TestFoo2(val x: Int) : KFoo2

inline class TestFoo3(val x: Int) : KFoo3

inline class TestBar1(val x: Int) : JIBar {
    override fun bar() {}
}

inline class TestBar2(val x: Int) : KBar2 {
    override fun bar() {}
}

// FILE: JIFoo.java
public interface JIFoo {
    default void foo() {}
}

// FILE: JIBar.java
public interface JIBar {
    default void foo() {}
    default void bar() {}
}
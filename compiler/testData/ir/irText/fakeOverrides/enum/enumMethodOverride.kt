// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK
// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR

// FILE: Java1.java
public interface Java1 {
    public void foo(Integer i);
}

// FILE: Java2.java
public interface Java2 extends KotlinInterface { }

// FILE: Java3.java
public interface Java3 {
    public void foo(Object i);
}

// FILE: 1.kt
enum class A : Java1    //Kotlin ← Java

enum class B : Java1 {  //Kotlin ← Java with explicit override
    FIRST {
        override fun foo(i: Int) { }
    },
    SECOND;
    override fun foo(i: Int) { }
}

enum class C: Java2 //Kotlin ← Java ← Kotlin

enum class D : Java2 {  //Kotlin ← Java ← Kotlin with explicit override
    FIRST {
        override fun foo(i: Int) {}
    };
    override fun foo(i: Int) { }
}

enum class E : Java1, Java3 //Kotlin ← Java1, Java2

enum class F: Java1, Java3 {    //Kotlin ← Java1, Java2 with explicit override
    FIRST {
        override fun foo(i: Int?) { }
    };
    override fun foo(i: Any) { }
}

interface KotlinInterface {
    fun foo(i: Int){}
}

fun test() {
    B.FIRST.foo(1)
    B.SECOND.foo(1)
    B.FIRST.ordinal
    D.FIRST.foo(1)
    D.FIRST.ordinal
    F.FIRST.foo(1)
    F.FIRST.foo("")
    F.FIRST.foo(null)
    F.FIRST.ordinal
}
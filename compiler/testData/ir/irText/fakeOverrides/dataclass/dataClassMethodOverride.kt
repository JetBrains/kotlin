// SKIP_KT_DUMP
// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FULL_JDK

// FILE: Java1.java
public class Java1 {
    public Object component1(){
        return "java1";
    };

    public void foo(Integer i){};
}

// FILE: Java2.java
public interface Java2 {
    public Integer component1();
    public void foo(Object i);
}

// FILE: Java3.java
public interface Java3 extends KotlinInterface { }

// FILE: 1.kt
data class A (val a : Int): Java1() {   //Kotlin ← Java
    override fun foo(i: Int?) { }
}

data class B(val a : Int) : Java1(), Java2 {    //Kotlin ← Java1, Java2
    override fun foo(i: Any?) { }
}

data class C(val a: Int): Java1(), KotlinInterface {    //Kotlin ← Java, Kotlin2
    override fun foo(i: Any) { }
}

data class D(val a: Int): Java3 {   //Kotlin ← Java ← Kotlin
    override fun foo(i: Any) { }
}


interface KotlinInterface {
    fun component1(): Int
    fun foo(i: Any)
}

fun test() {
    val k: Int = A(1).component1()
    A(1).foo(1)
    A(1).foo(null)

    val k2: Int = B(1).component1()
    B(1).foo(1)
    B(1).foo(null)

    val k3: Int = C(1).component1()
    C(1).foo(1)
    C(1).foo(null)

    val k4: Int = D(1).component1()
    D(1).foo(1)
}
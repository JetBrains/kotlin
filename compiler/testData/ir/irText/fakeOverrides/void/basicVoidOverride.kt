// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 {
    public Void foo(){
        return null;
    }
}

// FILE: 1.kt
class A : Java1()

class B : Java1() {
    override fun foo(): Void {
        return null!!
    }
}

fun test() {
    val k: Void? = A().foo()
    val k2: Any = A().foo()
    val k3: Void = B().foo()
    val k4: Any = B().foo()
}
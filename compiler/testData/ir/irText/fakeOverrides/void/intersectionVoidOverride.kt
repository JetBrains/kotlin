// SKIP_KT_DUMP
// TARGET_BACKEND: JVM

// FILE: Java1.java
public class Java1 {
    public Void foo(){
        return null;
    }
}

// FILE: Java2.java
public interface Java2  {
    public Object foo();
}

// FILE: 1.kt
class C : Java1(), Java2

class D : Java1(), Java2 {
    override fun foo(): Void {
        return null!!
    }
}

fun test() {
    val k1: Any = C().foo()
    val k2: Void = C().foo()
    val k3: Void = D().foo()
}
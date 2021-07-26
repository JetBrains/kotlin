// TARGET_BACKEND: JVM_IR

// FILE: j/JBase.java
package j;
interface JBase {
    void foo();
}

// FILE: j/JSub.java
package j;
public interface JSub extends JBase{}

// FILE: j/JOperation.java
package j;
public class JOperation {
    public void forJBase(JBase jb) {
        jb.foo();
    }
}

// FILE: k/JSub1.java
package k;
import j.JSub;
interface JSub1 extends JSub{}

// FILE: k/JSub1Impl.java
package k;
public class JSub1Impl implements JSub1{
    @Override
    public void foo() { }
}

// FILE: main.kt
package k
import j.JOperation
fun <T> test(t: T) where T : Any, T : JSub1 {
    JOperation().forJBase(t) // CHECKCAST to k/JSub1
}

fun box(): String {
    test(JSub1Impl())
    return "OK"
}
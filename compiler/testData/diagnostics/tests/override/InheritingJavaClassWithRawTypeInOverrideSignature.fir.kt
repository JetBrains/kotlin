// RUN_PIPELINE_TILL: BACKEND
// FILE: A.java
import java.util.List;

public interface A<T> {
    void foo(List<T> list);
}

// FILE: B.java
import java.util.List;

public abstract class B implements A<String> {
    @Override
    public final void foo(List list) {}
}

// FILE: C.java
public class C extends B implements A<String> {}

// FILE: Main.kt
class X : C() // false positive in K1, OK in K2

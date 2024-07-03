// DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java
import java.util.*;

public class A {
    void foo(List<Object> x) {}
    <T> void foo2(List<T> x) {}
    void foo3(Collection<? extends Object> x) {}

    List<? extends Number> bar() {}
    List<List<? extends Number>> bar2() {}
    ListHolder<? extends Number> bar3() {}
}

// FILE: ListHolder.java
import java.util.*;

public interface ListHolder<T> {
    List<T> get();
}

// FILE: B.java
import java.util.*;

public class B<T> {
    public B(Collection<T> c) {

    }
}

// FILE: main.kt

fun main(a: A) {
    a.foo(<!JAVA_TYPE_MISMATCH!>a.bar()<!>)
    a.foo2(a.bar())
    a.foo3(a.bar())
    B(a.bar())

    a.foo(<!JAVA_TYPE_MISMATCH!>a.bar2()<!>)
    a.foo2(a.bar2())
    a.foo3(a.bar2())
    B(a.bar2())

    a.foo(<!JAVA_TYPE_MISMATCH!>a.bar3().get()<!>)
    a.foo2(<!JAVA_TYPE_MISMATCH!>a.bar3().get()<!>)
    a.foo3(a.bar3().get())
    B(<!JAVA_TYPE_MISMATCH!>a.bar3().get()<!>)
}

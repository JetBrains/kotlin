// FIR_IDENTICAL
// FILE: JavaClass.java
import java.util.List;

public interface JavaClass<E> {
    List<? extends E> getFoo();
    void setFoo(List<? extends E> l);
}

// FILE: main.kt

fun foo(x: JavaClass<in CharSequence>, l: MutableList<out CharSequence>) {
    x.setFoo(l) // OK
    x.foo = l // Should be OK, too
}

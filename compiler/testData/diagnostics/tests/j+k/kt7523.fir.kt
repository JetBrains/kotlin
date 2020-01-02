// FILE: A.java
public interface A<T extends A<? super T, ?>, S extends A<? super T, ?>> {}

// FILE: C.java
public class C
{
    public <T extends A<? super T, ?>, S extends A<? super T, ?>> void f(A<T, S> x){}
}

// FILE: main.kt
fun foo() {
    // TODO: uncomment when KT-9597 is fixed
    // C().f(object : A<*, *> {})
}

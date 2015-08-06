// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass<Int>): Int {
    val inner = javaClass.createInner<String>()
    return inner.doSomething(1, "") { }
}

// FILE: JavaClass.java
public class JavaClass<T> {
    public <X> Inner<X> createInner() {
        return new Inner<X>();
    }

    public interface Inner<X>{
        public T doSomething(T t, X x, Runnable runnable);
    }
}

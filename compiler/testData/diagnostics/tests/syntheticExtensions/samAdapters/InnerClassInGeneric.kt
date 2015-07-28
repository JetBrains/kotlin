// FILE: KotlinFile.kt
fun foo(javaClass: JavaClass<Int>) {
    val inner = javaClass.createInner<String>()
    inner.doSomething(1, "") {
        bar()
    }
}

fun bar(){}

// FILE: JavaClass.java
public class JavaClass<T> {
    public <X> Inner<X> createInner() {
        return new Inner<X>();
    }

    public class Inner<X>{
        public void doSomething(T t, X x, Runnable runnable) { runnable.run(); }
    }
}

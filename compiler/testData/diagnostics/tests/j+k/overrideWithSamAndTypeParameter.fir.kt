// FILE: MyFunc.java

public interface MyFunc {
    String apply(String x);
}

// FILE: A.java
public interface A<K> {
    K foo(K key, MyFunc f);
}

// FILE: B.java
public class B<E> implements A<E> {
    @Override
    public E foo(E key, MyFunc f) {return null;}
}

// FILE: main.kt

fun main() {
    B<String>().foo("") { "" }
}

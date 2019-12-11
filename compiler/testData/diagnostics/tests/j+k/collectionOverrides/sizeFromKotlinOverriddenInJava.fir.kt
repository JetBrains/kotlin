// FILE: A.java

abstract public class A<F> extends MyList<F> {
    int getSize() { return 0; }
}

// FILE: main.kt

abstract class MyList<G> : Collection<G> {}

fun main(a: A<String>) {
    a.size
}

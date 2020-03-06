// FILE: Inv.java

public class Inv<T> {}

// FILE: A.java

public class A {
    public static Inv<String[]> KEY = new Inv<>();
}

// FILE: main.kt

fun test() {
    val key = A.KEY
}
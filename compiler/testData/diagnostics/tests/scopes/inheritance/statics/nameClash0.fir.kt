// FILE: A.java

public interface A {
    int field = 1;
}

// FILE: B.java

public interface B {
    String field = 1;
}

// FILE: E.java

public class E implements A, B {
}

// FILE: O.java

public class O implements A, B {
    public static double field = 1;
}

// FILE: test.kt

fun test() {
    A.field
    B.field

    E.<!AMBIGUITY!>field<!>
    O.field
}

// FILE: A.java

public interface A {
    int field = 1;
}

// FILE: B.java

public interface B {
    String field = 1;
}

// FILE: C.java

public interface C extends A {
}

// FILE: D.java

public interface D extends B {
}

// FILE: E.java

public class E implements C, D {
}

// FILE: EE.java

public class EE extends E {
}

// FILE: EO.java

public class EO extends E {
    public static double field = 1;
}

// FILE: O.java

public class O implements C, D {
    public static double field = 1;
}

// FILE: OO.java

public class OO extends O {
}

// FILE: test.kt

fun test() {
    A.field
    B.field

    C.field
    D.field

    E.<!OVERLOAD_RESOLUTION_AMBIGUITY!>field<!>
    O.field

    EE.<!OVERLOAD_RESOLUTION_AMBIGUITY!>field<!>
    EO.field

    OO.field
}

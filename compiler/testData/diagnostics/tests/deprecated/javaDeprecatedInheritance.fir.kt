// FILE: A.java

public class A {

    @Deprecated
    public static final String D = "d";

    @Deprecated
    public void f() {
        return text;
    }

    @Deprecated
    public static void bar() {
    }
}

// FILE: B.java

public class B extends A {

    public static final String D = "d";

    @Override
    public void f() {
        return text;
    }

    public static void bar() {
    }
}


// FILE: C.java

public class C extends A {
}

// FILE: use.kt

fun use(a: A, b: B, c: C) {
    a.f()
    b.f()
    c.f()

    A.D
    B.<!AMBIGUITY!>D<!>
    C.D

    A.bar()
    B.bar()
    C.bar()
}

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
    a.<!DEPRECATION!>f<!>()
    b.f()
    c.<!DEPRECATION!>f<!>()

    A.<!DEPRECATION!>D<!>
    B.D
    C.<!DEPRECATION!>D<!>

    A.<!DEPRECATION!>bar<!>()
    B.bar()
    C.<!DEPRECATION!>bar<!>()
}

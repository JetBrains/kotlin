// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

// FILE: SuperA.java

public class SuperA {
    public static A of(int x) {
        return null;
    }
}

// FILE: A.java

public class A extends SuperA {
    public static A of(int... x) {
        return null;
    }
}

// FILE: SuperB.java

public class SuperB {
    public static B of(int... x) {
        return null;
    }
}

// FILE: B.java

public class B extends SuperB {
    public static B of(int x) {
        return null;
    }
}

// FILE: SuperC.java

public class SuperC {
    public static SuperC of(int... x) {
        return null;
    }
}

// FILE: C.java

public class C extends SuperC {
}

// FILE: SuperD.java

public class SuperD {
    public static SuperD of(int... x) {
        return null;
    }
}

// FILE: D.java

public class D extends SuperD {
    public static D of(long... x) {
        return null;
    }
}

// FILE: test.kt

fun <T> accept(t: T) {}

fun test() {
    accept<A>(<!UNRESOLVED_REFERENCE!>[]<!>)
    accept<A>(<!UNRESOLVED_REFERENCE!>[1]<!>)
    accept<A>(<!UNRESOLVED_REFERENCE!>[1, 2]<!>)

    accept<B>(<!UNRESOLVED_REFERENCE!>[]<!>)
    accept<B>(<!UNRESOLVED_REFERENCE!>[1]<!>)
    accept<B>(<!UNRESOLVED_REFERENCE!>[1, 2]<!>)

    accept<C>(<!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>)

    accept<D>(<!UNRESOLVED_REFERENCE!>[42.toLong()]<!>)
    accept<D>(<!UNRESOLVED_REFERENCE!>[42.toInt()]<!>)
    accept<D>(<!UNRESOLVED_REFERENCE!>[42]<!>)
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, javaType, nullableType, typeParameter */

// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals +CompanionBlocksAndExtensions

// FILE: SuperA.java

public class SuperA {
    public static A of(int... x) {
        return null;
    }
}

// FILE: A.kt

class A : SuperA()

private fun test() {
    val a: A = <!UNRESOLVED_REFERENCE!>[42]<!>
}

// FILE: SuperB.java

public class SuperB {
    public static SuperB of(int... x) {
        return null;
    }
}

// FILE: B.kt

class B : SuperB()

private fun test() {
    val b: B = <!UNRESOLVED_REFERENCE!>[42]<!>
}

// FILE: SuperC.java

public class SuperC {
    public static SuperC of(int x) {
        return null;
    }

    public static C of(int x, int y) {
        return null;
    }
}

// FILE: C.kt

class C : SuperC() {
    companion {
        operator fun of(vararg a: Int): C = C()
    }
}

private fun test() {
    val c0: C = []
    val c1: C = [42]
    val c2: C = [42, 42]
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, javaType, localProperty, operator,
propertyDeclaration, vararg */

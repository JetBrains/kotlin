// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// JDK_KIND: FULL_JDK_11
//  ^ to enable private static of

// FILE: Base.java

public interface Base {
    public static Base of(int... x) {
        return null;
    }

    static Base of(int x) { // actually public, since it's interface
        return null;
    }

    private static Base of(int x, int y) {
        return null;
    }

    public static Base of(int x, int y, int z) {
        return null;
    }
}

// FILE: test.kt

fun <T> accept(t: T) {}

class Derived : Base {
    fun testDerived() {
        accept<Base>(<!UNRESOLVED_REFERENCE!>[]<!>)
        accept<Base>(<!UNRESOLVED_REFERENCE!>[1]<!>)
        accept<Base>(<!UNRESOLVED_REFERENCE!>[1, 2]<!>)
        accept<Base>(<!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>)
        accept<Base>(<!UNRESOLVED_REFERENCE!>[1, 2, 3, 4]<!>)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, functionDeclaration, integerLiteral, javaType, nullableType,
typeParameter */

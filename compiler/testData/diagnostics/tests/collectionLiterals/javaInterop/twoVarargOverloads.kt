// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

// FILE: DifferentTypes.java

public interface DifferentTypes {
    static DifferentTypes of(int... ints) {
        return null;
    }

    static DifferentTypes of(long... longs) {
        return null;
    }
}

// FILE: SameType.java

public interface SameType<T> {
    static <U> SameType<U> of() {
        return null;
    }

    static <U> SameType<U> of(U... us) {
        return null;
    }

    static <U> SameType<U> of(U u, U... us) {
        return null;
    }
}

// FILE: test.kt

fun test() {
    val a: DifferentTypes = <!UNRESOLVED_REFERENCE!>[]<!>
    val b: DifferentTypes = <!UNRESOLVED_REFERENCE!>[42L]<!>
    val c: DifferentTypes = <!UNRESOLVED_REFERENCE!>[42]<!>

    val d: SameType<Int> = <!UNRESOLVED_REFERENCE!>[]<!>
    val e: SameType<Int> = <!UNRESOLVED_REFERENCE!>[42]<!>
    val f: SameType<Int> = <!UNRESOLVED_REFERENCE!>[4, 2]<!>
    val g: SameType<*> = <!UNRESOLVED_REFERENCE!>["a", "b", "c"]<!>
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, javaType, localProperty,
propertyDeclaration, starProjection, stringLiteral */

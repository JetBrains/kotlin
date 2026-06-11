// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

// FILE: NonGeneric.java

public interface NonGeneric {
    static <U> NonGeneric of() {
        return null;
    }

    static <U> NonGeneric of(U u) {
        return null;
    }

    static <U> NonGeneric of(U... us) {
        return null;
    }
}

// FILE: Generic.java

public interface Generic<T> {
    static Generic<String> of() {
        return null;
    }

    static Generic<String> of(String str) {
        return null;
    }

    static Generic<String> of(String str1, String str2, String... strs) {
        return null;
    }
}

// FILE: test.kt

fun <T> accept(x: T) { }

fun test() {
    accept<NonGeneric>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<NonGeneric>(<!UNRESOLVED_COLLECTION_LITERAL!>["42"]<!>)
    accept<NonGeneric>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    accept<NonGeneric>(<!UNRESOLVED_COLLECTION_LITERAL!>[42, "42"]<!>)

    accept<Generic<*>>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<Generic<*>>(<!UNRESOLVED_COLLECTION_LITERAL!>["42"]<!>)
    accept<Generic<*>>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    accept<Generic<*>>(<!UNRESOLVED_COLLECTION_LITERAL!>["1", "2", "3"]<!>)
    accept<Generic<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<Generic<Int>>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<Generic<CharSequence>>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<Generic<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>["1", "2", "3"]<!>)
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, javaType, nullableType, starProjection,
stringLiteral, typeParameter */

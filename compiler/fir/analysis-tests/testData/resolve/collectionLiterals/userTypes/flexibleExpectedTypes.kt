// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

// FILE: Utils.java

public class Utils {
    public static void expectFlexible(C<String> arg) {
    }

    public static <K> void expectFlexibleGeneric(C<K> arg) {
    }

    public static <G> C<G> flexible() {
        return C<G>();
    }
}

// FILE: test.kt

class C<T> {
    companion object {
        operator fun <K> of(vararg ks: K): C<K> = C()
    }
}

fun <H> expectThroughTV(a: H, b: H) {}

fun test() {
    Utils.expectFlexible(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    Utils.expectFlexible(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)
    Utils.expectFlexible(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["42"]<!>)

    Utils.<!CANNOT_INFER_PARAMETER_TYPE!>expectFlexibleGeneric<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    Utils.<!CANNOT_INFER_PARAMETER_TYPE!>expectFlexibleGeneric<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42, "42"]<!>)
    Utils.expectFlexibleGeneric<String>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>expectThroughTV<!>(Utils.<!CANNOT_INFER_PARAMETER_TYPE!>flexible<!>(), <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)
    expectThroughTV(Utils.flexible<Int>(), <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)
    expectThroughTV(Utils.flexible<String>(), <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)
    expectThroughTV(Utils.flexible<String>(), <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, integerLiteral,
nullableType, objectDeclaration, operator, stringLiteral, typeParameter, vararg */

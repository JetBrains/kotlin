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
    Utils.expectFlexible([])
    Utils.expectFlexible(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    Utils.expectFlexible(["42"])

    Utils.<!CANNOT_INFER_PARAMETER_TYPE!>expectFlexibleGeneric<!>(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    Utils.expectFlexibleGeneric([42, "42"])
    Utils.expectFlexibleGeneric<String>([])

    expectThroughTV(Utils.flexible(), [42])
    expectThroughTV(Utils.flexible<Int>(), [42])
    expectThroughTV(Utils.flexible<String>(), [42])
    expectThroughTV(Utils.flexible<String>(), [])
}

/* GENERATED_FIR_TAGS: classDeclaration, collectionLiteral, companionObject, functionDeclaration, integerLiteral,
nullableType, objectDeclaration, operator, stringLiteral, typeParameter, vararg */

// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_COROUTINES

// FILE: MainIsNotSuspend.java
import kotlin.coroutines.Continuation;

public interface MainIsNotSuspend {
    static void of(Continuation<MainIsNotSuspend> cont) {
    }

    static MainIsNotSuspend of(int... ints) {
        return null;
    }
}

// FILE: test.kt

suspend fun test() {
    val y: MainIsNotSuspend = <!UNRESOLVED_COLLECTION_LITERAL!>[]<!>
    val t: MainIsNotSuspend = <!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, checkNotNullCall, classDeclaration, collectionLiteral,
companionObject, functionDeclaration, functionalType, integerLiteral, javaType, lambdaLiteral, localProperty,
nullableType, objectDeclaration, override, primaryConstructor, propertyDeclaration, safeCall, suspend, thisExpression,
typeParameter */

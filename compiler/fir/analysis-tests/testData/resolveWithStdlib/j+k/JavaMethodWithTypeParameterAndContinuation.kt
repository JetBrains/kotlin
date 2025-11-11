// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80549
// WITH_COROUTINES
// FILE: FooFunc.java
import kotlin.coroutines.Continuation;

public interface FooFunc {
    // Build would have succeeded if T does not "extend Object":
    // <T> Object foo(T param, Continuation<T> completion);
    <T extends Object> Object foo(T param, Continuation<T> completion);
}

// FILE: FooFuncKotlinUser.kt

object FooFuncKotlinUser {
    suspend fun <T : Any> useSite(myDelegate: FooFunc, myParam: T): Any? {
        return myDelegate.foo(myParam)
    }
}

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, checkNotNullCall, classDeclaration, companionObject,
flexibleType, functionDeclaration, functionalType, javaFunction, javaType, lambdaLiteral, localProperty, nullableType,
objectDeclaration, override, primaryConstructor, propertyDeclaration, safeCall, suspend, thisExpression, typeConstraint,
typeParameter */

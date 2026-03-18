// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-27831
// WITH_STDLIB

// FILE: Provider.java
import java.util.List;

public class Provider {
    static List<Inlay> getSomeInlays() { return null; }
}

// FILE: kt27831.kt
// KT-27831: False positive USELESS_CAST of raw generic type to non-raw one
class Inlay<T : Any> {
    internal fun <V> getUserData(inferMe: V): V {
        return inferMe
    }
}

fun test() {
    Provider.getSomeInlays().forEach { // it: Inlay<(raw) Any!>!
        it <!USELESS_CAST!>as Inlay<*>?<!> // reported as useless cast (false positive)
        it.getUserData(false)
    }
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, flexibleType, functionDeclaration, javaFunction, lambdaLiteral,
nullableType, starProjection, typeConstraint, typeParameter */

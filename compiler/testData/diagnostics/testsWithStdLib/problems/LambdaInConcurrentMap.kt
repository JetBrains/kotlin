// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-8761
// FULL_JDK
// FIR_DUMP

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

fun <K, V> ConcurrentMap<K, V>.forEachValue(action: (V) -> Unit) {}

fun main() {
    val map = ConcurrentHashMap<String, Int>()
    map.forEachValue { v ->
        // expected v to be `Int` here, got `Int!`
        v<!UNNECESSARY_SAFE_CALL!>?.<!>plus(2)
        v.minus(2)
    }
}

/* GENERATED_FIR_TAGS: flexibleType, funWithExtensionReceiver, functionDeclaration, functionalType, javaFunction,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, typeParameter */

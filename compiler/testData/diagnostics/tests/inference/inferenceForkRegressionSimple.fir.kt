// RUN_PIPELINE_TILL: BACKEND
// SKIP_TXT
// FULL_JDK
// WITH_STDLIB

interface C : MutableMap<String, Int>

fun <K, V> foo(m: MutableMap<K, V>, c: C) {
    if (c === m) {
        // `m` has a type C & MutableMap<K, V>
        // So it has two instances of Map supertypes: Map<String, Int> and Map<K, V>
        //
        // Thus, it leads to constraint system fork during `flatMap` inference
        // And during overload resolution between two `flatMap` versions with @OverloadResolutionByLambdaReturnType we have to run lambda analysis
        // So, we have to apply forks to the system on this completion phase too, so we would have enough information for the input types of lambda
        // Otherwise, OVERLOAD_RESOLUTION_AMBIGUITY happens
        // But we don't do it for PARTIAL completion mode still
        c.flatMap { _ ->
            listOf("")
        }
    }
}

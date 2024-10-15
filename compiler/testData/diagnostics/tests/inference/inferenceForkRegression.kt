// RUN_PIPELINE_TILL: BACKEND
// SKIP_TXT
// FIR_IDENTICAL
// FULL_JDK
// WITH_STDLIB

import java.util.concurrent.ConcurrentHashMap

interface ModificationData

fun main() {
    val updatedFiles = ConcurrentHashMap<String, ModificationData>()

    // HashMap effectively have two Map supertypes: one like Map<K, V> and one Map<K!, V!> (through AbstractMap)
    // But here, since `updatedFiles` has only a supertype Map<String!, ModificationData!>
    // `updatedFilesSnapshot` has two supertypes relevant to Map: Map<String!, ModificationData!>  and one Map<String!, ModificationData!>
    // Actually, we might collapse them, because they're equivalent, but for now we don't
    val updatedFilesSnapshot = HashMap(updatedFiles)

    // Thus, it leads to constraint system fork during `flatMap` inference
    // And during overload resolution between two `flatMap` versions with @OverloadResolutionByLambdaReturnType we have to run lambda analysis
    // So, we have to apply forks to the system on this completion phase too, but still not for the PARTIAL completion mode
    // Otherwise, OVERLOAD_RESOLUTION_AMBIGUITY happens
    updatedFilesSnapshot.flatMap { _ ->
        listOf("")
    }
}

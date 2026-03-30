// RUN_PIPELINE_TILL: BACKEND
// FULL_JDK
// WITH_STDLIB
// STDLIB_JDK8

import java.util.*
import kotlin.streams.toList

fun testStreams(list: ArrayList<String>) {
    list.stream().toList()
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration */

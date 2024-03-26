// WITH_STDLIB
// FULL_JDK
// FIR_DUMP
// ISSUE: KT-66784

import kotlin.jvm.optionals.getOrNull

val collection: Collection<*> = listOf(1, 2, 3)
val stream = collection.stream()
val first = stream.findFirst()
val result = first.<!NONE_APPLICABLE!>getOrNull<!>()

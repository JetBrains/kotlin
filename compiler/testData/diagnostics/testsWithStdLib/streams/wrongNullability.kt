// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
// JVM_TARGET: 1.8
// FIR_DUMP

import java.util.function.IntPredicate
import java.util.stream.Stream
import kotlin.streams.toList

class IntLongPair(val i: Int, val l: Long)

interface Process {
    fun pid(): Int

    fun totalCpuDuration(): Long?
}

fun run(filter: IntPredicate, allProcesses: Stream<Process>): List<IntLongPair> {
    return allProcesses.filter {
        filter.test(it.pid())
    }.map<IntLongPair?> {
        val duration = it.totalCpuDuration()
        if (duration != null) IntLongPair(it.pid(), <!DEBUG_INFO_SMARTCAST!>duration<!>)
        else null
    }.toList()
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, flexibleType, functionDeclaration, ifExpression,
inProjection, interfaceDeclaration, javaFunction, lambdaLiteral, localProperty, nullableType, outProjection,
primaryConstructor, propertyDeclaration, samConversion, smartcast */

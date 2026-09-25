// IGNORE_BACKEND: JKLIB
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK

// KT-89566 Reflection: original methods from explicit java.lang/java.util supertypes (instead of Kotlin mapped builtin classes) are absent in the new implementation
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: 1.kt
import java.util.*
import java.util.function.UnaryOperator
import kotlin.Comparator

abstract class A : LinkedList<Int>(), java.util.List<Int> {
    override fun spliterator(): Spliterator<Int> {
        return null!!
    }
    override fun sort(c: Comparator<in Int>?) { }
    override fun replaceAll(operator: UnaryOperator<Int>) { }
}

fun test(a: A){
    a.size
    a.add(1)
    a.get(1)
    a.remove()
    a.removeAt(1)
    a.remove(element = 1)
}
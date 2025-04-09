// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidOverloadClashesByErasure
// JDK_KIND: FULL_JDK_21
// WITH_STDLIB

// FILE: 1.kt
import java.util.*

class A : LinkedList<Int>(), SequencedCollection<Int>

class B : LinkedList<Int>(), SequencedCollection<Int> {
    override fun addFirst(e: Int?) { }
    override fun reversed(): LinkedList<Int> {
        return null!!
    }
}

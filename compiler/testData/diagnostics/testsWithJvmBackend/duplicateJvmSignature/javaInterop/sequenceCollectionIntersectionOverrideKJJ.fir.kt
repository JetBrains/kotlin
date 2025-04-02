// JDK_KIND: FULL_JDK_21
// WITH_STDLIB
// ISSUE: KT-13712

// FILE: 1.kt
import java.util.*

class A : LinkedList<Int>(), SequencedCollection<Int>

// Nullability of `e` parameter in LinkedList is Enhanced => B:addFirst does not override LinkedList:addFirst (from FIR POV).
// However, bridge created for SequencedCollection:addFirst has JVM signature `void addFirst(Object)` which overrides
// `void addFirst(Object)` in LinkedList on JVM.
<!CONFLICTING_INHERITED_JVM_DECLARATIONS!>class B : LinkedList<Int>(), SequencedCollection<Int> {
    override fun addFirst(e: Int?) { }
    override fun reversed(): LinkedList<Int> {
        return null!!
    }
}<!>

fun test(a: A, b: B){
    a.size
    a.remove(element = 1)
    a.addFirst(3)
    a.removeFirst()
    a.removeLast()
    a.reversed()

    b.reversed()
    b.addFirst(1)
}

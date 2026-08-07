// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)
// TODO: Review

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

// 'Iterator.next' is boxed in return position, 'Comparable.compareTo' in parameter position - the two
// remaining shapes next to the collection delegation in 'collectionDelegation.kt'.
@JvmExposeBoxed
class IteratorHolder(delegate: Iterator<Id>) : Iterator<Id> by delegate

@JvmExposeBoxed
class ComparableHolder(private val id: Id) : Comparable<Id> {
    override fun compareTo(other: Id): Int = id.value.compareTo(other.value)
}

@JvmExposeBoxed("newIteratorHolder")
fun makeIteratorHolder(): IteratorHolder = IteratorHolder(listOf(Id("OK")).iterator())

// FILE: Main.java
public class Main {
    public String iterator() {
        return ICKt.newIteratorHolder().next().getValue();
    }

    public int comparable() {
        return new ComparableHolder(new Id("a")).compareTo(new Id("a"));
    }
}

// FILE: Box.kt
fun box(): String {
    val res = Main().iterator()
    if (res != "OK") return "FAIL 1: $res"
    val comparison = Main().comparable()
    if (comparison != 0) return "FAIL 2: $comparison"
    return "OK"
}

// FULL_JDK
// FILE: MyIterable.java
public interface MyIterable<T> extends Iterable<T>

// FILE: test.kt
interface UseIterable : MyIterable<String> {
    fun test() {
        val it = iterator()
        val split = <!UNRESOLVED_REFERENCE!>spliterator<!>()
    }
}

fun test(some: Iterable<String>) {
    val it = some.iterator()
    val split = some.<!UNRESOLVED_REFERENCE!>spliterator<!>()
}

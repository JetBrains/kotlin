// FULL_JDK
// FILE: MyIterable.java
public interface MyIterable<T> extends Iterable<T>

// FILE: test.kt
interface UseIterable : MyIterable<String> {
    fun test() {
        val it = iterator()
        val split = spliterator()
    }
}

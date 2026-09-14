// TARGET_BACKEND: JVM
// FULL_JDK

// MODULE: lib
// FILE: lib.kt
interface MyIterable<T> : Iterable<T>

class StringIterable : MyIterable<String> {
    override fun iterator(): Iterator<String> = java.util.Collections.emptyIterator()
}

// MODULE: main(lib)
// FILE: main.kt
fun test(iterable: MyIterable<Int>, stringIterable: StringIterable) {
    iterable.spliterator()
    stringIterable.spliterator()
}

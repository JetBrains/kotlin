// ISSUE: KT-49652
// FILE: First.kt
package first

private typealias Key = String

// FILE: second/Second.java
package second

public class Second {
    public static class Key {
        public void foo() {}
    }

    public void bar() {}
}

// FILE: Third.kt
package first

import second.Second.*
import second.Second

open class Base<T> {
    fun get(): T? = null
}

// Key is resolved to first.Key. In fact, should be second.Second.Key, because first.Key is private-in-file
class Derived : Base<Key>()

fun test(d: Derived) {
    d.get()?.foo()
}

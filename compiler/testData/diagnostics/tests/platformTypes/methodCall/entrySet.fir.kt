// FILE: p/MultiMap.java

package p;

import java.util.*;

public class MultiMap<K, V> {
    public Set<Collection<V>> entrySet() {
        return null;
    }
}

// FILE: k.kt

import p.*

fun test() {
    val map = MultiMap<Int, String>()
    val set = map.entrySet()
    set.iterator()

    val set1 = map.entrySet()!!
    set1.iterator()
}
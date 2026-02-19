// FULL_JDK
// ISSUE: KT-67560

// FILE: main.kt
import java.util.concurrent.ConcurrentHashMap

fun <K, V> bar(key: K, value: V) {
    val a = ConcurrentHashMap<K, V>()
    a.put(key, value)
}

// FILE: java/util/concurrent/annotations.xml
<item name='java.util.concurrent.ConcurrentHashMap V put(K, V) 0'>
    <annotation name='org.jetbrains.annotations.NotNull'/>
</item>
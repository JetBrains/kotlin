// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.test.assertEquals

interface StringIntEntry : Map.Entry<String, Int>

interface EntryWithExtraTypeParameters<A, B, C : CharSequence, D : Number> : Map.Entry<A, B>

abstract class AbstractEntry<K, V> : Map.Entry<K, V>

class Impl1 : StringIntEntry {
    override val key: String get() = "key1"
    override val value: Int get() = 1
}

class Impl2 : EntryWithExtraTypeParameters<String, Int, String, Int> {
    override val key: String get() = "key2"
    override val value: Int get() = 2
}

class Impl3 : AbstractEntry<String, Int>() {
    override val key: String get() = "key3"
    override val value: Int get() = 3
}

fun box(): String {
    assertEquals("key1", StringIntEntry::key.call(Impl1()))
    assertEquals(1, StringIntEntry::value.call(Impl1()))
    assertEquals("key1", StringIntEntry::key.getter.call(Impl1()))
    assertEquals("key1", StringIntEntry::class.memberProperties.single { it.name == "key" }.call(Impl1()))
    assertEquals(1, StringIntEntry::class.memberProperties.single { it.name == "value" }.call(Impl1()))

    assertEquals("key2", EntryWithExtraTypeParameters<String, Int, String, Int>::key.call(Impl2()))
    assertEquals(2, EntryWithExtraTypeParameters<String, Int, String, Int>::value.call(Impl2()))
    val key2 = EntryWithExtraTypeParameters::class.memberProperties.single { it.name == "key" } as KProperty1<Impl2, *>
    assertEquals("key2", key2.get(Impl2()))

    assertEquals("key3", AbstractEntry<String, Int>::key.call(Impl3()))
    assertEquals(3, AbstractEntry<String, Int>::value.call(Impl3()))
    assertEquals("key3", AbstractEntry::class.memberProperties.single { it.name == "key" }.call(Impl3()))

    return "OK"
}

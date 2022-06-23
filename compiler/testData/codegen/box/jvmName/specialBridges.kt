// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// FILE: lib.kt
package lib

open class JvmNumber : Number() {
    @JvmName("byteValue")
    override fun toByte(): Byte = 1.toByte()

    @JvmName("shortValue")
    override fun toShort(): Short = 2.toShort()

    @JvmName("intValue")
    override fun toInt(): Int = 3

    @JvmName("longValue")
    override fun toLong(): Long = 4L

    @JvmName("floatValue")
    override fun toFloat(): Float = 5f

    @JvmName("doubleValue")
    override fun toDouble(): Double = 6.0

    // Doesn't exist on java.lang.Number
    override fun toChar(): Char = 'a'
}

class KotlinNumber : JvmNumber() {
    override fun toInt(): Int = 10
}

open class JvmCharSequence : CharSequence {
    @get:JvmName("length")
    override val length: Int
        get() = 0

    @JvmName("charAt")
    override fun get(index: Int): Char = 'a'

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = this
}

class KotlinCharSequence : JvmCharSequence() {
    override val length: Int
        get() = 1

    override fun get(index: Int): Char = super.get(index) + 1
}

open class JvmCollection<T> : Collection<T> {
    @get:JvmName("size")
    override val size: Int
        get() = 0

    override fun isEmpty(): Boolean = true
    override fun contains(element: T): Boolean = false
    override fun containsAll(elements: Collection<T>): Boolean = elements.isEmpty()
    override fun iterator(): Iterator<T> = TODO()
}

class KotlinCollection : JvmCollection<String>() {
    override val size: Int
        get() = 1
}

open class JvmMap<K,V> : Map<K,V> {
    @get:JvmName("size")
    override val size: Int
        get() = 0

    @get:JvmName("entrySet")
    override val entries: Set<Map.Entry<K, V>>
        get() = emptySet()

    @get:JvmName("values")
    override val values: Collection<V>
        get() = emptySet()

    @get:JvmName("keySet")
    override val keys: Set<K>
        get() = emptySet()

    override fun containsKey(key: K): Boolean = false
    override fun containsValue(value: V): Boolean = false
    override fun get(key: K): V? = null
    override fun isEmpty(): Boolean = true
}

class KotlinMap<K, V> : JvmMap<K, V>() {
    override val size: Int
        get() = super.size + 1

    override val entries: Set<Map.Entry<K, V>>
        get() = setOf()

    override val values: Collection<V>
        get() = listOf()

    override val keys: Set<K>
        get() = super.keys
}

// FILE: JavaNumber.java
package lib;

public class JavaNumber extends JvmNumber {
    public int intValue() { return 10; }

    public static long useKotlinNumber(KotlinNumber number) {
        return number.longValue();
    }
}

// FILE: JavaCharSequence.java
package lib;

public class JavaCharSequence extends JvmCharSequence {
    public int length() { return -1; }
    public char charAt(int index) { return (char) (super.charAt(index) + 2); }

    public static int useKotlinCharSequence(KotlinCharSequence s) {
        return s.length();
    }
}

// FILE: JavaCollection.java
package lib;

public class JavaCollection<T> extends JvmCollection<T> {
    public int size() { return -1; }

    public static int useKotlinCollection(KotlinCollection c) {
        return c.size() + 1;
    }
}

// FILE: JavaMap.java
package lib;

public class JavaMap extends JvmMap<String, String> {
    public int size() { return 2; }

    public static int useKotlinMap(KotlinMap<Integer, String> c) {
        return c.size() + 1;
    }
}

// FILE: test.kt
import lib.*

fun box(): String {
    val number = JvmNumber()
    if (number.toByte() != 1.toByte()) return "Fail 1"
    if (number.toShort() != 2.toShort()) return "Fail 2"
    if (number.toInt() != 3) return "Fail 3"
    if (number.toLong() != 4L) return "Fail 4"
    if (number.toFloat() != 5f) return "Fail 5"
    if (number.toDouble() != 6.0) return "Fail 6"

    val kotlinNumber = KotlinNumber()
    if (kotlinNumber.toInt() != 10) return "Fail 7"
    if (kotlinNumber.toByte() != 1.toByte()) return "Fail 8"

    val charSequence = JvmCharSequence()
    if (charSequence.length != 0) return "Fail 9"
    if (charSequence[0] != 'a') return "Fail 10"

    val kotlinCharSequence = KotlinCharSequence()
    if (kotlinCharSequence.length != 1) return "Fail 11"
    if (kotlinCharSequence[0] != 'b') return "Fail 12"

    val collection = JvmCollection<String>()
    if (collection.size != 0) return "Fail 13"

    val kotlinCollection = KotlinCollection()
    if (kotlinCollection.size != 1) return "Fail 14"

    val map = JvmMap<Int, String>()
    if (map.size != 0) return "Fail 15"
    if (!map.entries.isEmpty()) return "Fail 16"
    if (!map.keys.isEmpty()) return "Fail 17"
    if (!map.values.isEmpty()) return "Fail 18"

    val kotlinMap = KotlinMap<Int, String>()
    if (kotlinMap.size != 1) return "Fail 19"
    if (!kotlinMap.entries.isEmpty()) return "Fail 20"
    if (!kotlinMap.values.isEmpty()) return "Fail 21"
    if (!kotlinMap.keys.isEmpty()) return "Fail 22"

    // Java tests
    val javaNumber = JavaNumber()
    if (javaNumber.toInt() != 10) return "Fail 23"
    if (JavaNumber.useKotlinNumber(kotlinNumber) != 4L) return "Fail 24"

    val javaCharSequence = JavaCharSequence()
    if (javaCharSequence.length != -1) return "Fail 25"
    if (javaCharSequence[0] != 'c') return "Fail 26"
    if (JavaCharSequence.useKotlinCharSequence(kotlinCharSequence) != 1) return "Fail 27"

    val javaCollection = JavaCollection<String>()
    if (javaCollection.size != -1) return "Fail 28"
    if (JavaCollection.useKotlinCollection(kotlinCollection) != 2) return "Fail 29"

    val javaMap = JavaMap()
    if (javaMap.size != 2) return "Fail 30"
    if (JavaMap.useKotlinMap(kotlinMap) != 2) return "Fail 31"

    return "OK"
}

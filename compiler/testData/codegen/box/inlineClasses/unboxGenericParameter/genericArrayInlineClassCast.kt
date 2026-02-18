// ISSUE: KT-67409
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND: ANDROID
// ^ KT-52706
// LANGUAGE: +JvmInlineMultiFieldValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class KoneArray<E>(val array: Array<out E>)

fun KoneArray<Int>.raw0Int(): Array<Any?> {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Any?>
    return array
}

fun KoneArray<Int>.raw1Int(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Any?>
    return array
}

fun KoneArray<Int>.raw2Int(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Any?>?
    return array
}

fun KoneArray<Int>.raw3Int(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Any?>?
    return array
}

fun KoneArray<Int>.raw4Int(): Array<Int> {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Int>
    return array
}

fun KoneArray<Int>.raw5Int(): Array<Int>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Int>
    return array
}

fun KoneArray<Int>.raw6Int(): Array<Int>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Int>?
    return array
}

fun KoneArray<Int>.raw7Int(): Array<Int>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Int>?
    return array
}

fun KoneArray<UInt>.raw0UInt(): Array<Any?> {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Any?>
    return array
}

fun KoneArray<UInt>.raw1UInt(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Any?>
    return array
}

fun KoneArray<UInt>.raw2UInt(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Any?>?
    return array
}

fun KoneArray<UInt>.raw3UInt(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Any?>?
    return array
}

fun KoneArray<UInt>.raw4UInt(): Array<UInt> {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<UInt>
    return array
}

fun KoneArray<UInt>.raw5UInt(): Array<UInt>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<UInt>
    return array
}

fun KoneArray<UInt>.raw6UInt(): Array<UInt>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<UInt>?
    return array
}

fun KoneArray<UInt>.raw7UInt(): Array<UInt>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<UInt>?
    return array
}

fun KoneArray<IntArray>.raw0IntArray(): Array<Any?> {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Any?>
    return array
}

fun KoneArray<IntArray>.raw1IntArray(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Any?>
    return array
}

fun KoneArray<IntArray>.raw2IntArray(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Any?>?
    return array
}

fun KoneArray<IntArray>.raw3IntArray(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Any?>?
    return array
}

fun KoneArray<IntArray>.raw4IntArray(): Array<IntArray> {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<IntArray>
    return array
}

fun KoneArray<IntArray>.raw5IntArray(): Array<IntArray>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<IntArray>
    return array
}

fun KoneArray<IntArray>.raw6IntArray(): Array<IntArray>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<IntArray>?
    return array
}

fun KoneArray<IntArray>.raw7IntArray(): Array<IntArray>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<IntArray>?
    return array
}

fun KoneArray<UIntArray>.raw0UIntArray(): Array<Any?> {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Any?>
    return array
}

fun KoneArray<UIntArray>.raw1UIntArray(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Any?>
    return array
}

fun KoneArray<UIntArray>.raw2UIntArray(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Any?>?
    return array
}

fun KoneArray<UIntArray>.raw3UIntArray(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Any?>?
    return array
}

fun KoneArray<UIntArray>.raw4UIntArray(): Array<UIntArray> {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<UIntArray>
    return array
}

fun KoneArray<UIntArray>.raw5UIntArray(): Array<UIntArray>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<UIntArray>
    return array
}

fun KoneArray<UIntArray>.raw6UIntArray(): Array<UIntArray>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<UIntArray>?
    return array
}

fun KoneArray<UIntArray>.raw7UIntArray(): Array<UIntArray>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<UIntArray>?
    return array
}

fun KoneArray<Array<Int>>.raw0ArrayInt(): Array<Any?> {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Any?>
    return array
}

fun KoneArray<Array<Int>>.raw1ArrayInt(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Any?>
    return array
}

fun KoneArray<Array<Int>>.raw2ArrayInt(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Any?>?
    return array
}

fun KoneArray<Array<Int>>.raw3ArrayInt(): Array<Any?>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Any?>?
    return array
}

fun KoneArray<Array<Int>>.raw4ArrayInt(): Array<Array<Int>> {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Array<Int>>
    return array
}

fun KoneArray<Array<Int>>.raw5ArrayInt(): Array<Array<Int>>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Array<Int>>
    return array
}

fun KoneArray<Array<Int>>.raw6ArrayInt(): Array<Array<Int>>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as Array<Array<Int>>?
    return array
}

fun KoneArray<Array<Int>>.raw7ArrayInt(): Array<Array<Int>>? {
    @Suppress("UNCHECKED_CAST")
    val array = this.array as? Array<Array<Int>>?
    return array
}

fun testInt() {
    val array = Array(10) { it }
    val expectedContent = array.toList()
    val koneArray = KoneArray(array)
    koneArray.raw0Int().toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw1Int()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw2Int()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw3Int()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw4Int().toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw5Int()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw6Int()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw7Int()?.toList().let { require(it == expectedContent) { it.toString() } }
}

fun testUInt() {
    val array = Array(10) { it.toUInt() }
    val expectedContent = array.toList()
    val koneArray = KoneArray(array)
    koneArray.raw0UInt().toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw1UInt()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw2UInt()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw3UInt()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw4UInt().toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw5UInt()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw6UInt()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw7UInt()?.toList().let { require(it == expectedContent) { it.toString() } }
}

fun testIntArray() {
    val array = Array(10) { intArrayOf(it) }
    val expectedContent = array.toList()
    val koneArray = KoneArray(array)
    koneArray.raw0IntArray().toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw1IntArray()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw2IntArray()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw3IntArray()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw4IntArray().toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw5IntArray()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw6IntArray()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw7IntArray()?.toList().let { require(it == expectedContent) { it.toString() } }
}

fun testUIntArray() {
    val array = Array(10) { uintArrayOf(it.toUInt()) }
    val expectedContent = array.toList()
    val koneArray = KoneArray(array)
    koneArray.raw0UIntArray().toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw1UIntArray()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw2UIntArray()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw3UIntArray()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw4UIntArray().toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw5UIntArray()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw6UIntArray()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw7UIntArray()?.toList().let { require(it == expectedContent) { it.toString() } }
}

fun testArrayInt() {
    val array = Array(10) { arrayOf(it) }
    val expectedContent = array.toList()
    val koneArray = KoneArray(array)
    koneArray.raw0ArrayInt().toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw1ArrayInt()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw2ArrayInt()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw3ArrayInt()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw4ArrayInt().toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw5ArrayInt()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw6ArrayInt()?.toList().let { require(it == expectedContent) { it.toString() } }
    koneArray.raw7ArrayInt()?.toList().let { require(it == expectedContent) { it.toString() } }
}

fun box(): String {
    testInt()
    testUInt()
    testIntArray()
    testUIntArray()
    testArrayInt()
    
    return "OK"
}

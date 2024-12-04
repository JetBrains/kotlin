// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

fun box(): String {
    noUnderflow()
    testByteArray()
    testCharArray()
    testShortArray()
    testIntArray()
    testLongArray()
    testFloatArray()
    testDoubleArray()
    testBooleanArray()
    testEmptyList()
    testList()
    testMutableList()
    testCharSequence()
    testString()
    testEmptySet()
    testSet()
    testMutableSet()
    testEmptyMap()
    testMap()
    testMutableMap()
    return "OK"
}

fun noUnderflow() {
    val M1 = Int.MAX_VALUE - 2
    val M2 = Int.MIN_VALUE
    var t = 0
    for (x in M1..M2 - 1) {
        ++t
        assert(t <= 3) { "Failed: too many iterations" }
    }
    assert(t == 3) { "Failed: t=$t" }
}

fun testByteArray() {
    val array = byteArrayOf(1, 2, 3)
    val range = array.size - 1
    var optimized = 0
    var nonOptimized = 0
    for (i in 0..array.size - 1) optimized += array[i]
    for (i in 0..range) nonOptimized += array[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testCharArray() {
    val array = charArrayOf('1', '2', '3')
    val range = array.size - 1
    var optimized = ""
    var nonOptimized = ""
    for (i in 0..array.size - 1) optimized += array[i]
    for (i in 0..range) nonOptimized += array[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testShortArray() {
    val array = shortArrayOf(1, 2, 3)
    val range = array.size - 1
    var optimized = 0
    var nonOptimized = 0
    for (i in 0..array.size - 1) optimized += array[i]
    for (i in 0..range) nonOptimized += array[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testIntArray() {
    val array = intArrayOf(1, 2, 3)
    val range = array.size - 1
    var optimized = 0
    var nonOptimized = 0
    for (i in 0..array.size - 1) optimized += array[i]
    for (i in 0..range) nonOptimized += array[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testLongArray() {
    val array = longArrayOf(1, 2, 3)
    val range = array.size - 1
    var optimized = 0L
    var nonOptimized = 0L
    for (i in 0..array.size - 1) optimized += array[i]
    for (i in 0..range) nonOptimized += array[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testFloatArray() {
    val array = floatArrayOf(1f, 2f, 3f)
    val range = array.size - 1
    var optimized = 0f
    var nonOptimized = 0f
    for (i in 0..array.size - 1) optimized += array[i]
    for (i in 0..range) nonOptimized += array[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testDoubleArray() {
    val array = doubleArrayOf(1.0, 2.0, 3.0)
    val range = array.size - 1
    var optimized = 0.0
    var nonOptimized = 0.0
    for (i in 0..array.size - 1) optimized += array[i]
    for (i in 0..range) nonOptimized += array[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testBooleanArray() {
    val array = booleanArrayOf(true, false, true)
    val range = array.size - 1
    var optimized = ""
    var nonOptimized = ""
    for (i in 0..array.size - 1) optimized += array[i]
    for (i in 0..range) nonOptimized += array[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testEmptyList() {
    val list = emptyList<Int>()
    val range = list.size - 1
    var optimized = 0
    var nonOptimized = 0
    for (i in 0..list.size - 1) optimized += list[i]
    for (i in 0..range) nonOptimized += list[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testList() {
    val list = listOf(1, 2, 3)
    val range = list.size - 1
    var optimized = 0
    var nonOptimized = 0
    for (i in 0..list.size - 1) optimized += list[i]
    for (i in 0..range) nonOptimized += list[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testMutableList() {
    val list = mutableListOf(1, 2, 3)
    val range = list.size - 1
    var optimized = 0
    var nonOptimized = 0
    for (i in 0..list.size - 1) optimized += list[i]
    for (i in 0..range) nonOptimized += list[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testCharSequence() {
    val chars: CharSequence = "123"
    val range = chars.length - 1
    var optimized = ""
    var nonOptimized = ""
    for (i in 0..chars.length - 1) optimized += chars[i]
    for (i in 0..range) nonOptimized += chars[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testString() {
    val str = "123"
    val range = str.length - 1
    var optimized = ""
    var nonOptimized = ""
    for (i in 0..str.length - 1) optimized += str[i]
    for (i in 0..range) nonOptimized += str[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testEmptySet() {
    val set = emptySet<Int>()
    val range = set.size - 1
    var optimized = ""
    var nonOptimized = ""
    for (i in 0..set.size - 1) optimized += set.elementAt(i)
    for (i in 0..range) nonOptimized += set.elementAt(i)
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testSet() {
    val set = setOf(1, 2, 3)
    val range = set.size - 1
    var optimized = ""
    var nonOptimized = ""
    for (i in 0..set.size - 1) optimized += set.elementAt(i)
    for (i in 0..range) nonOptimized += set.elementAt(i)
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testMutableSet() {
    val set = mutableSetOf(1, 2, 3)
    val range = set.size - 1
    var optimized = ""
    var nonOptimized = ""
    for (i in 0..set.size - 1) optimized += set.elementAt(i)
    for (i in 0..range) nonOptimized += set.elementAt(i)
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testEmptyMap() {
    val map = emptyMap<Int, Int>()
    val range = map.size - 1
    var optimized = ""
    var nonOptimized = ""
    for (i in 0..map.size - 1) optimized += map[i]
    for (i in 0..range) nonOptimized += map[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testMap() {
    val map = mapOf(1 to 1, 2 to 2, 3 to 3)
    val range = map.size - 1
    var optimized = ""
    var nonOptimized = ""
    for (i in 0..map.size - 1) optimized += map[i]
    for (i in 0..range) nonOptimized += map[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}

fun testMutableMap() {
    val map = mutableMapOf(1 to 1, 2 to 2, 3 to 3)
    val range = map.size - 1
    var optimized = ""
    var nonOptimized = ""
    for (i in 0..map.size - 1) optimized += map[i]
    for (i in 0..range) nonOptimized += map[i]
    assert(optimized == nonOptimized) { "optimized($optimized) and nonOptimized($nonOptimized) should be equal" }
}
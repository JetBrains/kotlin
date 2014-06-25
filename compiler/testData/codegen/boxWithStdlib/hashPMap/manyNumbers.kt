import java.util.*
import kotlin.reflect.jvm.internal.pcollections.HashPMap
import kotlin.test.*

fun digitSum(number: Int): Int {
    var x = number
    var ans = 0
    while (x != 0) {
        ans += x % 10
        x /= 10
    }
    return ans
}

val N = 1000000

fun box(): String {
    var map = HashPMap.empty<Int, Any>()!!

    for (x in 1..N) {
        map = map.plus(x, digitSum(x))!!
    }

    assertEquals(N, map.size())
    
    // Check in reverse order just in case
    for (x in N downTo 1) {
        assertTrue(map.containsKey(x), "Not found: $x")
        assertEquals(digitSum(x), map[x], "Incorrect value for $x")
    }

    // Delete in random order
    val list = (1..N).toCollection(ArrayList<Int>())
    Collections.shuffle(list, Random(42))
    for (x in list) {
        map = map.minus(x)!!
    }

    assertEquals(0, map.size())

    for (x in 1..N) {
        assertFalse(map.containsKey(x), "Incorrectly found: $x")
        assertEquals(null, map[x], "Incorrectly found value for $x")
    }

    return "OK"
}

// WITH_STDLIB

import kotlin.test.assertEquals

fun facWhile(i: Int): Int {
    var count = 1;
    var result = 1;
    while(count < i) {
        count = count + 1;
        result = result * count;
    }
    return result;
}

fun facBreak(i: Int): Int {
    var count = 1;
    var result = 1;
    while(true) {
        count = count + 1;
        result = result * count;
        if (count == i) break;
    }
    return result;
}

fun facDoWhile(i: Int): Int {
    var count = 1;
    var result = 1;
    do {
        count = count + 1;
        result = result * count;
    } while(count != i);
    return result;
}

fun box(): String {
    assertEquals(6, facWhile(3))
    assertEquals(6, facBreak(3))
    assertEquals(6, facDoWhile(3))
    assertEquals(120, facWhile(5))
    assertEquals(120, facBreak(5))
    assertEquals(120, facDoWhile(5))
    return "OK"
}

import kotlin.test.assertEquals

fun foo(): Int {
    return 1
    // val xyz has empty live range because everything after return will be removed as dead
    val xyz = 1
}

fun box(): String {
    assertEquals(1, foo())
    return "OK"
}

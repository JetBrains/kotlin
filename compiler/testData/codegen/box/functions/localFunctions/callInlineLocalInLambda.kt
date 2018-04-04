// WITH_RUNTIME

import kotlin.test.assertEquals


fun run(r: () -> Any) = r()


fun test(i: Int, j: Int, k: Int): String {


    var i = 0
    var j = 2
    val k = 4

    fun f(): String = "OK"

    val funVal = {
        println(k)
    }
    run(funVal)

    val funLit = { i += 1
        j += k
        f() }
    val ret = run(funLit) as String
    if (i != 1 || j != 6 || k != 4) return "fail"
    return ret
}

fun box(): String {
    return test(1, 2, 3)
}

//fun box(): String {
////    fun bar(y: String) = y + "cde"
//
////    val res = foo("abc") { bar(it) }
//
////    assertEquals("abccde", res)
//
//    return "OK"
//}

import kotlin.platform.platformStatic

object A {

    platformStatic var a: Int = 1

    var b: Int = 1
     [platformStatic] get

    var c: Int = 1
        [platformStatic] set

}

fun box(): String {

    A.a++
    if (A.a != 2) return "fail 1"

    A.b++
    if (A.b != 2) return "fail 2"

    A.c++
    if (A.c != 2) return "fail 3"

    return "OK"
}
import kotlin.platform.platformStatic

object A {

    platformStatic var a: Int = 1

    var b: Int = 1
     [platformStatic] get

    var c: Int = 1
        [platformStatic] set

}



fun box(): String {
    if (A.test1() != "OK") return "fail 1"

    if (A.test2() != "OK") return "fail 2"

    if (A.test3() != "1OK") return "fail 3"

    if (A.test4() != "1OK") return "fail 4"

    if (with(A) {"1".test5()} != "1OK") return "fail 5"

    if (A.c != "OK") return "fail 6"

    return "OK"
}
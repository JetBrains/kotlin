import kotlin.platform.platformStatic

object A {

    val b: String = "OK"

    @platformStatic val c: String = "OK"

    @platformStatic fun test1() : String {
        return {b}()
    }

    @platformStatic fun test2() : String {
        return {test1()}()
    }

    fun test3(): String {
        return {"1".test5()}()
    }

    @platformStatic fun test4(): String {
        return {"1".test5()}()
    }

    @platformStatic fun String.test5() : String {
        return {this + b}()
    }

    fun test6(): String {
        return {c}()
    }
}

fun box(): String {
    if (A.test1() != "OK") return "fail 1"

    if (A.test2() != "OK") return "fail 2"

    if (A.test3() != "1OK") return "fail 3"

    if (A.test4() != "1OK") return "fail 4"

    if (with(A) {"1".test5()} != "1OK") return "fail 5"

    if (A.test6() != "OK") return "fail 6"

    return "OK"
}
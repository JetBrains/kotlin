// !LANGUAGE: -ProhibitAssigningSingleElementsToVarargsInNamedForm
// IGNORE_BACKEND_FIR: JVM_IR

fun box(): String {
    if (test1(p = 1) != "1") return "fail 1"
    if (test2(p = "1") != "1") return "fail 2"
    if (test3(p = "1") != "1") return "fail 3"

    return "OK"
}

fun test1(vararg p: Int): String {
    var result = ""
    for (i in p) {
        result += i
    }
    return result
}

fun test2(vararg p: String): String {
    var result = ""
    for (i in p) {
        result += i
    }
    return result
}

fun <T> test3(vararg p: T): String {
    var result = ""
    for (i in p) {
        result += i
    }
    return result
}
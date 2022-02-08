fun box(): String {
    if (test1() != "") return "fail 1"
    if (test1(1) != "1") return "fail 2"
    if (test1(1, 2) != "12") return "fail 3"

    if (test1(*intArrayOf()) != "") return "fail 4"
    if (test1(*intArrayOf(1)) != "1") return "fail 5"
    if (test1(*intArrayOf(1, 2)) != "12") return "fail 6"

    if (test1(p = *intArrayOf()) != "") return "fail 8"
    if (test1(p = *intArrayOf(1)) != "1") return "fail 9"
    if (test1(p = *intArrayOf(1, 2)) != "12") return "fail 10"

    if (test2() != "") return "fail 11"
    if (test2("1") != "1") return "fail 12"
    if (test2("1", "2") != "12") return "fail 13"

    if (test2(*arrayOf<String>()) != "") return "fail 14"
    if (test2(*arrayOf<String>("1")) != "1") return "fail 15"
    if (test2(*arrayOf<String>("1", "2")) != "12") return "fail 16"

    if (test2(p = *arrayOf<String>()) != "") return "fail 18"
    if (test2(p = *arrayOf<String>("1")) != "1") return "fail 19"
    if (test2(p = *arrayOf<String>("1", "2")) != "12") return "fail 20"

    if (test3<String>() != "") return "fail 21"
    if (test3("1") != "1") return "fail 22"
    if (test3("1", "2") != "12") return "fail 23"

    if (test3(*arrayOf<String>()) != "") return "fail 24"
    if (test3(*arrayOf<String>("1")) != "1") return "fail 25"
    if (test3(*arrayOf<String>("1", "2")) != "12") return "fail 26"

    if (test3(p = *arrayOf<String>()) != "") return "fail 28"
    if (test3(p = *arrayOf<String>("1")) != "1") return "fail 29"
    if (test3(p = *arrayOf<String>("1", "2")) != "12") return "fail 30"

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
class A(var value: String)

fun box(): String {
    val a = A("start")

    try {
        test(a)
    } catch(e: RuntimeException) {

    }

    if (a.value != "start, try, finally1, finally2") return "fail: ${a.value}"

    return "OK"
}

fun test(a: A) : String {
    while (a.value == "start") {
        try {
            try {
                a.value += ", try"
                continue
            }
            finally {
                a.value += ", finally1"
            }
        }
        finally {
            a.value += ", finally2"
            throw RuntimeException("fail")
        }
    }
    return "fail"
}

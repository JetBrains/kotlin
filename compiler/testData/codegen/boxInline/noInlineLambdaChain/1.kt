import test.*

fun test1(param: String): String {
    var result = "fail"
    inlineFun("1")  { c ->
        {
            inlineFun("2") { a ->
                {
                    {
                        result = param + c + a
                    }()
                }()
            }
        }()
    }

    return result
}


fun test2(param: String): String {
    var result = "fail"
    inlineFun("2") { a ->
        {
            {
                result = param + a
            }()
        }()
    }

    return result
}


fun box(): String {
    if (test1("start") != "start12") return "fail1: ${test1("start")}"
    if (test2("start") != "start2") return "fail2: ${test2("start")}"

    return "OK"
}
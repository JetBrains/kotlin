// FILE: 1.kt

package test

inline fun <T> inlineFun(arg: T, f: (T) -> Unit) {
    f(arg)
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
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

fun test3(param: String): String {
    var result = "fail"
    inlineFun("2") { d ->
        inlineFun("1") { c ->
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
    }

    return result
}


fun box(): String {
    if (test1("start") != "start12") return "fail1: ${test1("start")}"
    if (test2("start") != "start2") return "fail2: ${test2("start")}"
    if (test3("start") != "start12") return "fail3: ${test3("start")}"

    return "OK"
}

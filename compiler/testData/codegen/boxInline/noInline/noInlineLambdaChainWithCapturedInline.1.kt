import test.*

import kotlin.InlineOption.*

inline fun test1(inlineOptions(ONLY_LOCAL_RETURN) param: () -> String): String {
    var result = "fail"
    inlineFun("1")  { c ->
        {
            inlineFun("2") { a ->
                {
                    {
                        result = param() + c + a
                    }()
                }()
            }
        }()
    }

    return result
}


inline fun test2(inlineOptions(ONLY_LOCAL_RETURN) param: () -> String): String {
    var result = "fail"
    inlineFun("2") { a ->
        {
            {
                result = param() + a
            }()
        }()
    }

    return result
}

inline fun test3(inlineOptions(ONLY_LOCAL_RETURN) param: () -> String): String {
    var result = "fail"
    inlineFun("2") { d ->
        inlineFun("1") { c ->
            {
                inlineFun("2") { a ->
                    {
                        {
                            result = param() + c + a
                        }()
                    }()
                }
            }()
        }
    }

    return result
}


fun box(): String {
    if (test1({"start"}) != "start12") return "fail1: ${test1({"start"})}"
    if (test2({"start"}) != "start2") return "fail2: ${test2({"start"})}"
    if (test3({"start"}) != "start12") return "fail3: ${test3({"start"})}"

    var captured1 = "sta";
    val captured2 = "rt";
    if (test1({captured1 + captured2}) != "start12") return "fail4: ${test1({captured1 + captured2})}"
    if (test2({captured1 + captured2}) != "start2") return "fail5: ${test2({captured1 + captured2})}"
    if (test3({captured1 + captured2}) != "start12") return "fail6: ${test3({captured1 + captured2})}"

    return {
        if (test1 { captured1 + captured2 } != "start12") "fail7: ${test1 { captured1 + captured2 }}"
        else if (test2 { captured1 + captured2 } != "start2") "fail8: ${test2 { captured1 + captured2 }}"
        else if (test3 { captured1 + captured2 } != "start12") "fail9: ${test3 { captured1 + captured2 }}"
        else "OK"
    } ()

}
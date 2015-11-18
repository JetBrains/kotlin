package test

class A {
    val param = "start"
    var result = "fail"
    var addParam = "_additional_"

    inline fun inlineFun(arg: String, crossinline f: (String) -> Unit) {
        {
            f(arg + addParam)
        }()
    }


    fun box(): String {
        {
            inlineFun("2") { a ->
                {
                    result = param + a
                }()
            }
        }()
        return if (result == "start2_additional_") "OK" else "fail: $result"
    }

}
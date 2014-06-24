import test.*
import test.simpleJavaInnerEnum.MyEnum.A

fun box() =
    if (simpleJavaInnerEnum.MyEnum.A.toString() == "A" && A.toString() == "A") "OK"
    else "fail"

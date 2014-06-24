import test.simpleJavaEnumWithStaticImport.A

fun box() =
    if (A.toString() == "A") "OK"
    else "fail"

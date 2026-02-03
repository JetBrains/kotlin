class Foo {
    fun memberFun(): String = ""
}

fun topLevelFun(): String = ""

fun box(): String {
    if (::topLevelFun.equals(null)) throw AssertionError("::topLevelFun.equals(null) should be false")
    if (::topLevelFun.equals("not a callable")) throw AssertionError("::topLevelFun.equals(\"not a callable\") should be false")
    if (::topLevelFun.equals(42)) throw AssertionError("::topLevelFun.equals(42) should be false")
    if (Foo::memberFun.equals(null)) throw AssertionError("Foo::memberFun.equals(null) should be false")

    return "OK"
}

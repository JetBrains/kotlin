fun box(): String {
    val test1 = FakePlatformName().foo()
    if (test1 != "foo") return "Failed: FakePlatformName().foo()==$test1"

    return "OK"
}
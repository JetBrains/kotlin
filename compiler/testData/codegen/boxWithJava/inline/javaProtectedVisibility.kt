
public inline fun test(): String {
    val p = object : Test() {}
    return p.data + Test.testStatic();
}


fun box(): String {
    return test()
}
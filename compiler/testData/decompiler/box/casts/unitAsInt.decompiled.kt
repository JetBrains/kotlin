fun foo() {
}

fun box() : String  {
    try {
    (foo() as Int?)}
catch (e : ClassCastException)  {
    return "OK"
}
catch (e : Throwable)  {
    return "Fail: ClassCastException should have been thrown, but was instead ${javaClass.getName()}: ${e.message}"
}
    return "Fail: no exception was thrown"
}

// FIR_IDENTICAL

annotation class TestAnn(val x: String)

val test1: String
    @TestAnn("test1.get") get() = ""

var test2: String
    @TestAnn("test2.get") get() = ""
    @TestAnn("test2.set") set(value) {}

@get:TestAnn("test3.get")
val test3: String = ""

@get:TestAnn("test4.get")
@set:TestAnn("test4.set")
var test4: String = ""

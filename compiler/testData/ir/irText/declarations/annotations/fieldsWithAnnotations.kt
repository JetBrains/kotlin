// FIR_IDENTICAL

annotation class TestAnn(val x: String)

@field:TestAnn("testVal.field")
val testVal = "a val"

@field:TestAnn("testVar.field")
var testVar = "a var"

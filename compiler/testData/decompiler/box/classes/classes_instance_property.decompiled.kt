class TestClassWithProperty{
    constructor() {
    }
    val idx_val
    var idx_var
}
fun box(): String {
    val testClassWithProperty: TestClassWithProperty = TestClassWithProperty()
    when {
        testClassWithProperty.<get-idx_val>() == 11 -> {
            return "FAIL"
        }
    }
    return "OK"
}

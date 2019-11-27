// IGNORE_BACKEND_FIR: JVM_IR
class TestObject()
{
    companion object {
        var prop: Int = 1
            get() = field++
    }
}

fun box(): String {

    if (TestObject.prop != 1) return "fail 1"

    if (TestObject.prop != 2) return "fail 2"

    return "OK"
}
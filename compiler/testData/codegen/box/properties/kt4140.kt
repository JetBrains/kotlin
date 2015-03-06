class TestObject()
{
    default object {
        var prop: Int = 1
            get() = $prop++
    }
}

fun box(): String {

    if (TestObject.prop != 1) return "fail 1"

    if (TestObject.prop != 2) return "fail 2"

    return "OK"
}
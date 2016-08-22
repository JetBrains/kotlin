class Ref(var value: Int)

interface IHost {
    fun String.extLength() = length
}

fun test1(x: String?) = x?.length
fun test2(x: String?) = x?.hashCode()
fun test3(x: String?, y: Any?) = x?.equals(y)

fun test4(x: Ref?) {
    x?.value = 0
}

fun IHost.test5(s: String?) = s?.extLength()
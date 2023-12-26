// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR
// KJS_WITH_FULL_RUNTIME
// JVM_ABI_K1_K2_DIFF: KT-63864

val list = ArrayList<String>()

interface I2 {
    val size: Int
}

class B2 : ArrayList<String>(list), I2

interface I3<T> {
    val size: T
}

class B3 : ArrayList<String>(list), I3<Int>

fun box(): String {
    list.add("1")

    val b2 = B2()
    if (b2.size != 1) return "fail 3: ${b2.size}"
    var x: Collection<String> = B2()
    if (x.size != 1) return "fail 4: ${x.size}"
    val i2: I2 = b2
    if (i2.size != 1) return "fail 5: ${i2.size}"

    val b3 = B3()
    if (b3.size != 1) return "fail 6: ${b3.size}"
    x = B3()
    if (x.size != 1) return "fail 7: ${x.size}"
    val i3: I3<Int> = b3
    if (i3.size != 1) return "fail 8: ${i3.size}"

    return "OK"
}

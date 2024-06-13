// LANGUAGE: +EliminateAmbiguitiesWithExternalTypeParameters
// WITH_STDLIB

class AllCollection<T> {
    fun addAll1(vararg values: T) = "NOK"
    fun addAll1(values: Iterable<out T>) = "OK"

    fun addAll2(vararg values: Any) = "NOK"
    fun addAll2(values: Any) = "OK"

    fun addAll3(vararg values: T) = "NOK"
    fun addAll3(values: T) = "OK"

    fun addAll4(vararg values: T) = "NOK"
    fun addAll4(values: Collection<T>) = "OK"

    fun addAll5(vararg values: Any) = "NOK"
    fun addAll5(values: Collection<T>) = "OK"

    fun addAll6(vararg values: Collection<T>) = "NOK"
    fun addAll6(values: Collection<T>) = "OK"

    fun addAll7(vararg values: Collection<T>) = "OK"
    fun addAll7(values: T) = "NOK"

    fun addAll8(vararg values: Collection<T>) = "OK"
    fun addAll8(values: Any) = "NOK"

    fun addAll9(vararg values: Collection<String>) = "OK"
    fun addAll9(values: Any) = "NOK"

    fun addAll10(vararg values: Collection<String>) = "OK"
    fun addAll10(values: T) = "NOK"

    fun addAll11(vararg values: Collection<String>) = "NOK"
    fun addAll11(values: Collection<String>) = "OK"

    fun addAll12(vararg values: Collection<String>) = "OK"
    fun addAll12(values: Collection<T>) = "NOK"

    fun addAll13(vararg values: Any) = "NOK"
    fun addAll13(values: Collection<String>) = "OK"

    fun addAll14(vararg values: T) = "NOK"
    fun addAll14(values: Collection<String>) = "OK"

    fun addAll15(vararg values: Collection<T>) = "NOK"
    fun addAll15(values: Collection<String>) = "OK"


    fun addAll16(vararg values: Collection<String>) = "NOK"
    fun addAll16(values: Collection<String>, values2: Collection<String>) = "OK"

    fun addAll17(vararg values: Collection<String>) = "OK"
    fun addAll17(values: Collection<T>, values2: Collection<T>) = "NOK"

    fun addAll18(vararg values: Any) = "NOK"
    fun addAll18(values: Collection<String>, values2: Collection<String>) = "OK"

    fun addAll19(vararg values: T) = "NOK"
    fun addAll19(values: Collection<String>, values2: Collection<String>) = "OK"

    fun addAll20(vararg values: Collection<T>) = "NOK"
    fun addAll20(values: Collection<String>, values2: Collection<String>) = "OK"


    fun addAll21(vararg values: Collection<T>) = "NOK"
    fun addAll21(values: Collection<String>, values2: Collection<String>) = "OK"

    // KT-49620
    fun addAll22(vararg values: Collection<String>) = "OK"
    fun addAll22(values: Any, values2: Any) = "NOK"

    fun addAll23(vararg values: Collection<String>) = "OK"
    fun addAll23(values: T, values2: T) = "NOK"


    fun addAll24(values: Collection<T>, vararg values2: Collection<T>) = "NOK"
    fun addAll24(values: Collection<String>, values2: Collection<String>) = "OK"

    // KT-49620
    fun addAll25(values: Collection<String>, vararg values2: Collection<String>) = "OK"
    fun addAll25(values: Any, values2: Any) = "NOK"

    fun addAll26(values: Collection<String>, vararg values2: Collection<String>) = "OK"
    fun addAll26(values: T, values2: T) = "NOK"

    // KT-49534
    fun <K, T> addAll28(vararg values: T, values2: K) = "NOK" // 1
    fun <K, T> addAll28(values: K, vararg values2: T) = "OK" // 2
}

fun box(): String {
    val c: AllCollection<Any?> = AllCollection()
    val x1 = c.addAll1(listOf(""))
    val x2 = c.addAll2(listOf(""))
    val x3 = c.addAll3(listOf(""))
    val x4 = c.addAll4(listOf(""))
    val x5 = c.addAll5(listOf(""))
    val x6 = c.addAll6(listOf(""))
    val x7 = c.addAll7(listOf(""))
    val x8 = c.addAll8(listOf(""))
    val x9 = c.addAll9(listOf(""))
    val x10 = c.addAll10(listOf(""))
    val x11 = c.addAll11(listOf(""))
    val x12 = c.addAll12(listOf(""))
    val x13 = c.addAll13(listOf(""))
    val x14 = c.addAll14(listOf(""))
    val x15 = c.addAll15(listOf(""))
    val x16 = c.addAll16(listOf(""), listOf(""))
    val x17 = c.addAll17(listOf(""), listOf(""))
    val x18 = c.addAll18(listOf(""), listOf(""))
    val x19 = c.addAll19(listOf(""), listOf(""))
    val x20 = c.addAll20(listOf(""), listOf(""))
    val x21 = c.addAll21(listOf(""), listOf(""))
    val x22 = c.addAll22(listOf(""), listOf(""))
    val x23 = c.addAll23(listOf(""), listOf(""))
    val x24 = c.addAll24(listOf(""), listOf(""))
    val x25 = c.addAll25(listOf(""), listOf(""))
    val x26 = c.addAll26(listOf(""), listOf(""))

    val all = arrayOf(x1, x2, x3, x4, x5, x6, x7, x8, x9, x10, x11, x12, x13, x14, x15, x16, x17, x18, x19, x20, x21, x22, x23, x24, x25, x26)

    return if (all.all { it == "OK" }) "OK" else "NOK"
}

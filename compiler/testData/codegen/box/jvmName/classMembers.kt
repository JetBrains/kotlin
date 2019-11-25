// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME
// See:
// http://kotlinlang.org/docs/reference/java-interop.html#handling-signature-clashes-with-platformname
// https://youtrack.jetbrains.com/issue/KT-5524

val strs = listOf("abc", "def")
val ints = listOf(1, 2, 3)

class C {
    // Instance methods

    @JvmName("instMethodStr")
    fun instMethod(list: List<String>): String = "instMethodStr"

    @JvmName("instMethodInt")
    fun instMethod(list: List<Int>): String = "instMethodInt"

    // Properties

    var rwProperty: Int
        @JvmName("get_rwProperty")
        get() = 123
        @JvmName("set_rwProperty")
        set(v) {}

    var rwValue = 111

    fun getRwProperty(): Int = rwValue

    fun setRwProperty(v: Int) {
        rwValue = v
    }

    // Extension methods

    class Inner

    @JvmName("extMethodWithGenericParamStr")
    fun Inner.extMethodWithGenericParam(list: List<String>): String = "extMethodWithGenericParamStr"

    @JvmName("extMethodWithGenericParamInt")
    fun Inner.extMethodWithGenericParam(list: List<Int>): String = "extMethodWithGenericParamInt"

    // This is already covered by extMethodWithGenericParam(), but might be relevant for a platform
    // with extension method code generation strategy different from Java 6.

    @JvmName("extMethodWithGenericReceiverStr")
    fun List<String>.extMethodWithGenericReceiver(): String = "extMethodWithGenericReceiverStr"

    @JvmName("extMethodWithGenericReceiverInt")
    fun List<Int>.extMethodWithGenericReceiver(): String = "extMethodWithGenericReceiverInt"

    // Extension method vs instance method

    @JvmName("ambigMethod1")
    fun ambigMethod(str: String): String = "ambigMethod1"

    @JvmName("ambigMethod2")
    fun String.ambigMethod(): String = "ambigMethod2"

}

fun box(): String {
    val c = C()

    // Instance methods: 
    // method signatures with erased types SHOULD NOT clash

    val test1 = c.instMethod(strs)
    if (test1 != "instMethodStr") return "Fail: c.instMethod(strs)==$test1"

    val test2 = c.instMethod(ints)
    if (test2 != "instMethodInt") return "Fail: c.instMethod(ints)==$test2"

    // Properties: 
    // property accessors SHOULD NOT clash with class methods  

    val test3 = c.rwProperty
    if (test3 != 123) return "Fail: c.rwProperty==$test3"

    val test3a = c.getRwProperty()
    if (test3a != 111) return "Fail: c.getRwProperty()==$test3a"

    c.setRwProperty(444)
    val test3b = c.rwProperty
    if (test3b != 123) return "Fail: c.rwProperty==$test3b after c.setRwProperty(1234)"
    val test3c = c.getRwProperty()
    if (test3c != 444) return "Fail: c.getRwProperty()==$test3c after c.setRwProperty(1234)"

    // Extension methods:
    // method signatures with erased types SHOULD NOT clash

    val test4 = with(c) { C.Inner().extMethodWithGenericParam(strs) }
    if (test4 != "extMethodWithGenericParamStr") return "Fail: with(c) { C.Inner().extMethodWithGenericParam(strs) }==$test4"

    val test5 = with(c) { C.Inner().extMethodWithGenericParam(ints) }
    if (test5 != "extMethodWithGenericParamInt") return "Fail: with(c) { C.Inner().extMethodWithGenericParam(ints) }==$test5"

    val test6 = with(c) { strs.extMethodWithGenericReceiver() }
    if (test6 != "extMethodWithGenericReceiverStr") return "Fail: with(c) { strs.extMethodWithGenericReceiver() }==$test6"

    val test7 = with(c) { ints.extMethodWithGenericReceiver() }
    if (test7 != "extMethodWithGenericReceiverInt") return "Fail: with(c) { ints.extMethodWithGenericReceiver() }==$test7"

    // Extension method SHOULD NOT clash with instance method with the same Java signature.

    val str = "abc"

    val test8 = with(c) { ambigMethod(str) }
    if (test8 != "ambigMethod1") return "Fail: with(c) { ambigMethod(str) }==$test8"

    val test9 = with(c) { str.ambigMethod() }
    if (test9 != "ambigMethod2") return "Fail: with(c) { str.ambigMethod() }==$test9"

    // Everything is fine.

    return "OK"
}

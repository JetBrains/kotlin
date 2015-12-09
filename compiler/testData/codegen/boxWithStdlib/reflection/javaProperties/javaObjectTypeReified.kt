import kotlin.reflect.KClass

inline fun <reified T : Any> check(expected: String) {
    val clazz = T::class.javaObjectType!!
    assert (clazz.canonicalName == "java.lang.${expected.capitalize()}") {
        "clazz name: ${clazz.canonicalName}"
    }
}

fun box(): String {
    check<Boolean>("boolean")
    check<Char>("character")
    check<Byte>("byte")
    check<Short>("short")
    check<Int>("integer")
    check<Float>("float")
    check<Long>("long")
    check<Double>("double")

    check<String>("String")
    check<java.lang.Void>("Void")

    return "OK"
}

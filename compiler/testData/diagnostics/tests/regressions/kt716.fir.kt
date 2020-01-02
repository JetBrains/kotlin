// KT-716 Type inference failed

class TypeInfo<T>

fun <T> typeinfo() : TypeInfo<T> = null as TypeInfo<T>

fun <T> TypeInfo<T>.getJavaClass() : java.lang.Class<T> {
    val t : java.lang.Object = this as java.lang.Object
    return t.getClass() as java.lang.Class<T> // inferred type is Object but Serializable was expected
}

fun <T> getJavaClass() = typeinfo<T>().getJavaClass()

fun main() {
    System.out.println(getJavaClass<String>())
}
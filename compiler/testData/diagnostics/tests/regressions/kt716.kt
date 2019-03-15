// KT-716 Type inference failed

class TypeInfo<T>

fun <T> typeinfo() : TypeInfo<T> = null <!CAST_NEVER_SUCCEEDS!>as<!> TypeInfo<T>

fun <T> TypeInfo<T>.getJavaClass() : java.lang.Class<T> {
    val t : <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Object<!> = this as <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Object<!>
    return t.getClass() <!UNCHECKED_CAST!>as java.lang.Class<T><!> // inferred type is Object but Serializable was expected
}

fun <T> getJavaClass() = typeinfo<T>().getJavaClass()

fun main() {
    System.out.println(getJavaClass<String>())
}
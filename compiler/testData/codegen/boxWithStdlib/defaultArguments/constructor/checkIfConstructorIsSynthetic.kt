class A(value: Int = 1)

fun box(): String {
    val constructors = javaClass<A>().getConstructors().filter { !it.isSynthetic() }
    return if (constructors.size() == 2) "OK" else constructors.size().toString()
}

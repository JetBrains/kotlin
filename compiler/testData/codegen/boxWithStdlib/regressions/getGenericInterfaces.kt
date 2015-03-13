// KT-4485 getGenericInterfaces vs getInterfaces for kotlin classes

class SimpleClass

class ClassWithNonGenericSuperInterface: Cloneable

class ClassWithGenericSuperInterface: java.util.Comparator<String> {
    override fun compare(a: String, b: String): Int = 0
}

fun check(klass: Class<*>) {
    val interfaces = klass.getInterfaces().toList()
    val genericInterfaces = klass.getGenericInterfaces().toList()
    if (interfaces.size != genericInterfaces.size) {
        throw AssertionError("interfaces=$interfaces, genericInterfaces=$genericInterfaces")
    }
}

fun box(): String {
    check(javaClass<SimpleClass>())
    check(javaClass<ClassWithNonGenericSuperInterface>())
    check(javaClass<ClassWithGenericSuperInterface>())
    return "OK"
}

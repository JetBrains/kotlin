// WITH_REFLECT
// FILE: J.java
import java.util.List;

public class J<T> {
    public String[] array() { return null; }
    public String[][] arrayOfArray() { return null; }
    public void arrayParameter(String[] a) {}
    public List<String>[] arrayOfList() { return null; }
    public T[] arrayOfT() { return null; }
}

// FILE: JRaw.java
public class JRaw extends J {}

// FILE: box.kt
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure
import kotlin.test.assertEquals

fun check(expected: Class<*>, type: KType) {
    assertEquals(expected, (type.classifier as KClass<*>).java)
    assertEquals(expected, type.jvmErasure.java)
    assertEquals(expected, type.javaType)
}

fun checkElement(expected: Class<*>, type: KType) {
    val element = type.arguments.single().type!!
    assertEquals(expected, (element.classifier as KClass<*>).java)
    assertEquals(expected, element.jvmErasure.java)
}

fun member(name: String) = JRaw::class.members.single { it.name == name }

fun box(): String {
    check(Array<String>::class.java, member("array").returnType)
    check(Array<Array<String>>::class.java, member("arrayOfArray").returnType)
    check(Array<String>::class.java, member("arrayParameter").parameters[1].type)

    checkElement(String::class.java, member("array").returnType)
    checkElement(Array<String>::class.java, member("arrayOfArray").returnType)
    checkElement(String::class.java, member("arrayOfArray").returnType.arguments.single().type!!)
    checkElement(String::class.java, member("arrayParameter").parameters[1].type)
    checkElement(List::class.java, member("arrayOfList").returnType)
    checkElement(Any::class.java, member("arrayOfT").returnType)
    return "OK"
}

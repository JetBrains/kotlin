// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: TypedJavaBase.java
public class TypedJavaBase<T> {
    public T value() { return null; }
    public void setValue(T t) {}
    public java.util.List<T> getList() { return null; }
}

// FILE: box.kt
// Tests that a Kotlin class extending a generic Java class has its inherited fake override
// methods with type parameters correctly substituted with the concrete type argument.
// See also referenceToInheritedMembersInJava.kt for the opposite direction (Java class extending a generic Kotlin class).

import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

class KotlinExtendsTyped : TypedJavaBase<String>()
class KotlinExtendsTypedInt : TypedJavaBase<Int>()

private val useK1 = Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true

private fun KClass<*>.member(name: String) = members.single { it.name == name }

fun box(): String {
    val value = KotlinExtendsTyped::class.member("value")
    assertEquals("fun KotlinExtendsTyped.value(): kotlin.String!", value.toString())
    assertEquals(null, value.call(KotlinExtendsTyped()))
    if (useK1) {
        // KT-87366: javaType of fake overrides is not substituted in the K1-based implementation.
        assertEquals("T", value.returnType.javaType.toString())
        return "OK"
    }
    assertEquals(String::class.java, value.returnType.javaType)

    val setValue = KotlinExtendsTyped::class.member("setValue")
    assertEquals("fun KotlinExtendsTyped.setValue(kotlin.String!): kotlin.Unit", setValue.toString())
    assertEquals(String::class.java, setValue.parameters[1].type.javaType)

    val getList = KotlinExtendsTyped::class.member("getList")
    assertEquals("fun KotlinExtendsTyped.getList(): kotlin.collections.(Mutable)List<kotlin.String!>!", getList.toString())
    assertEquals("java.util.List<java.lang.String>", getList.returnType.javaType.toString())

    // Substitution with a primitive-mapped type must use the wrapper class.
    val intValue = KotlinExtendsTypedInt::class.member("value")
    assertEquals("fun KotlinExtendsTypedInt.value(): kotlin.Int!", intValue.toString())
    assertEquals(Int::class.javaObjectType, intValue.returnType.javaType)

    return "OK"
}

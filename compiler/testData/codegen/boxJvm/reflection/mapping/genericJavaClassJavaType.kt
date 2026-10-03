// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK
// FILE: GenericJavaHolder.java
public class GenericJavaHolder {
    public static class Box<T> {
        public T get() { return null; }
        public void set(T value) {}
    }
    public static class Pair<A, B> {
        public A first() { return null; }
        public B second() { return null; }
    }
}

// FILE: box.kt
// Tests that KType.javaType of a Java class type parameter usage is the corresponding TypeVariable of that class.

import java.lang.reflect.TypeVariable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaType
import kotlin.test.*

private fun checkTypeVariable(type: KType, owner: KClass<*>, index: Int) {
    val javaType = type.javaType
    assertTrue(javaType is TypeVariable<*>, "Expected a type variable: $javaType")
    assertEquals(owner.java.typeParameters[index], javaType)
    assertEquals(owner.typeParameters[index], type.classifier)
}

fun box(): String {
    val box = GenericJavaHolder.Box::class
    checkTypeVariable(box.memberFunctions.single { it.name == "get" }.returnType, box, 0)
    checkTypeVariable(box.memberFunctions.single { it.name == "set" }.valueParameters.single().type, box, 0)

    val pair = GenericJavaHolder.Pair::class
    checkTypeVariable(pair.memberFunctions.single { it.name == "first" }.returnType, pair, 0)
    checkTypeVariable(pair.memberFunctions.single { it.name == "second" }.returnType, pair, 1)

    return "OK"
}

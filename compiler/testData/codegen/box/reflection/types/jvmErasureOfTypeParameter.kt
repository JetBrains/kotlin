// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.KClass
import kotlin.test.assertEquals

open class O

class A {
    fun <T> simple(): T = null!!
    fun <T : String> string(): T = null!!
    fun <T : String?> nullableString(): T = null!!
    fun <T : U, U> otherTypeParameter(): T = null!!
    fun <T : U, U : List<String>> otherTypeParameterWithBound(): T = null!!

    fun <T : Cloneable> twoInterfaces1(): T where T : Comparable<*> = null!!
    fun <T : Comparable<*>> twoInterfaces2(): T where T : Cloneable = null!!
    fun <T : Cloneable> interfaceAndClass1(): T where T : O = null!!
    fun <T : O> interfaceAndClass2(): T where T : Cloneable = null!!

    fun <T> arrayOfAny(): Array<T> = null!!
    fun <T : Number> arrayOfNumber(): Array<T> = null!!
    fun <T> arrayOfArrayOfCloneable(): Array<Array<T>> where T : Cloneable, T : Comparable<*> = null!!
}

fun get(name: String): KClass<*> = A::class.members.single { it.name == name }.returnType.jvmErasure

fun box(): String {
    assertEquals(Any::class, get("simple"))
    assertEquals(String::class, get("string"))
    assertEquals(String::class, get("nullableString"))
    assertEquals(Any::class, get("otherTypeParameter"))
    assertEquals(List::class, get("otherTypeParameterWithBound"))

    assertEquals(Cloneable::class, get("twoInterfaces1"))
    assertEquals(Comparable::class, get("twoInterfaces2"))
    assertEquals(O::class, get("interfaceAndClass1"))
    assertEquals(O::class, get("interfaceAndClass2"))

    assertEquals(Array<Any>::class, get("arrayOfAny"))
    assertEquals(Array<Number>::class, get("arrayOfNumber"))
    assertEquals(Array<Array<Cloneable>>::class, get("arrayOfArrayOfCloneable"))

    return "OK"
}

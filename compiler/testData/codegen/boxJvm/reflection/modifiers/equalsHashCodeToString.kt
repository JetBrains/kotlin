// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: box.kt
import kotlin.reflect.KFunction
import kotlin.test.assertFalse
import kotlin.test.assertTrue

interface Base {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
}

interface Sub : Base

abstract class C : Base

fun checkAbstract(f: KFunction<*>) {
    assertTrue(f.isAbstract, "$f should be abstract")
    assertFalse(f.isOpen, "$f should not be open")
    assertFalse(f.isFinal, "$f should not be final")
}

fun box(): String {
    for (name in listOf("equals", "hashCode", "toString")) {
        checkAbstract(Base::class.members.single { it.name == name } as KFunction<*>)
        checkAbstract(Sub::class.members.single { it.name == name } as KFunction<*>)
        checkAbstract(C::class.members.single { it.name == name } as KFunction<*>)
    }

    return "OK"
}

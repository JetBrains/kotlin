// TARGET_BACKEND: JVM
// WITH_REFLECT
// FORCE_STDLIB_ONLY_REFLECTION

import kotlin.reflect.KClass

class A
object Obj

fun isLight(klass: KClass<*>): Boolean =
    try {
        klass.members
        false
    } catch (e: KotlinReflectionNotSupportedError) {
        // expected
        true
    }

inline fun <reified T: Any> getKClass(): KClass<T> {
    return T::class
}

fun box(): String {
    val a = A()
    if (!isLight(A::class)) return "Failed for A::class"
    if (!isLight(a::class)) return "Failed for a::class"
    if (!isLight(Obj::class)) return "Failed for Obj::class"
    if (!isLight(Int::class)) return "Failed for Int::class"
    if (!isLight(getKClass<A>())) return "Failed for getKClass<A>()"
    if (!isLight(a.javaClass.kotlin)) return "Failed for a.javaClass.kotlin"
    if (!isLight(Class.forName("A").kotlin)) return "Failed for Class.forName(\"A\").kotlin"

    return "OK"
}

// CHECK_BYTECODE_TEXT
// 0 INVOKESTATIC kotlin/jvm/internal/Reflection.getOrCreateKotlinClass
// 8 INVOKESTATIC kotlin/jvm/internal/StdlibOnlyReflection.getOrCreateKotlinClass
// 0 INVOKESTATIC kotlin/jvm/JvmClassMappingKt.getKotlinClass

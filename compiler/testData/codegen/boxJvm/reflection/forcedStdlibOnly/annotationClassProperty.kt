// TARGET_BACKEND: JVM
// WITH_REFLECT
// FORCE_STDLIB_ONLY_REFLECTION

import kotlin.reflect.KClass

annotation class MyAnnoWithKClass(val klass: KClass<*>)
annotation class MyAnnoWithKClassArray(val klasses: Array<KClass<*>>)

fun testCompile1(anno: MyAnnoWithKClass) {
    println(anno.klass)
}

fun testCompile2(anno: MyAnnoWithKClassArray) {
    println(anno.klasses)
}

fun box() = "OK"

// CHECK_BYTECODE_TEXT
// 0 INVOKESTATIC kotlin/jvm/internal/Reflection.getOrCreateKotlinClass
// 1 INVOKESTATIC kotlin/jvm/internal/StdlibOnlyReflection.getOrCreateKotlinClass \(Ljava/lang/Class;\)Lkotlin/reflect/KClass;
// 1 INVOKESTATIC kotlin/jvm/internal/StdlibOnlyReflection.getOrCreateKotlinClasses \(\[Ljava/lang/Class;\)\[Lkotlin/reflect/KClass;

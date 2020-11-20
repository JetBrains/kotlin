// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: ccc.kt

@file:JvmName("Facade")
@file:JvmMultifileClass
package test
fun ccc() {}

// FILE: aaa.kt

@file:JvmName("Facade")
@file:JvmMultifileClass
package test
fun aaa() {}

// FILE: _b.kt

@file:JvmName("Facade")
@file:JvmMultifileClass
package test
fun b() {}

// FILE: test.kt

fun box(): String {
    val names = Class.forName("test.Facade").getAnnotation(Metadata::class.java).data1.toList()
    return if (names == listOf("test/Facade__AaaKt", "test/Facade__CccKt", "test/Facade___bKt")) "OK" else "Fail: $names"
}

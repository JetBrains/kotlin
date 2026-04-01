// TARGET_BACKEND: JVM
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
    // Account for package renaming in Android tests
    return if (names == listOf("test.Facade__AaaKt".replace(".", "/"),
                               "test.Facade__CccKt".replace(".", "/"),
                               "test.Facade___bKt".replace(".", "/"))) "OK" else "Fail: $names"
}

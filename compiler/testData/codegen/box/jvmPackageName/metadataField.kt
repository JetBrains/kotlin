// TARGET_BACKEND: JVM
// WITH_RUNTIME
// LANGUAGE_VERSION: 1.2

// FILE: foo.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:JvmPackageName("baz.foo.quux.bar")
package foo.bar

fun f() {}

// FILE: bar.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

fun getPackageName(classFqName: String): String =
        Class.forName(classFqName).getAnnotation(Metadata::class.java).pn

fun box(): String {
    val bar = getPackageName("BarKt")
    if (bar != "") return "Fail 1: $bar"

    val foo = getPackageName("baz.foo.quux.bar.FooKt")
    if (foo != "foo.bar") return "Fail 2: $foo"

    return "OK"
}

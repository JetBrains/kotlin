// !LANGUAGE: +MultiPlatformProjects
// !USE_EXPERIMENTAL: kotlin.ExperimentalMultiplatform
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: common.kt

@OptionalExpectation
expect annotation class Anno(val s: String)

// FILE: jvm.kt

import java.lang.reflect.AnnotatedElement

@Anno("Foo")
class Foo @Anno("<init>") constructor(@Anno("x") x: Int) {
    @Anno("bar")
    fun bar() {}

    @Anno("getX")
    var x = x
        @Anno("setX")
        set

    @Anno("Nested")
    interface Nested
}

private fun check(element: AnnotatedElement) {
    check(element.annotations)
}

private fun check(annotations: Array<Annotation>) {
    val filtered = annotations.filterNot { it.annotationClass.java.name == "kotlin.Metadata" }
    if (filtered.isNotEmpty()) {
        throw AssertionError("Annotations should be empty: $filtered")
    }
}

fun box(): String {
    val foo = Foo::class.java
    check(foo)
    check(Foo.Nested::class.java)
    check(foo.declaredMethods.single { it.name == "bar" })
    check(foo.declaredMethods.single { it.name == "getX" })
    check(foo.declaredMethods.single { it.name == "setX" })
    check(foo.constructors.single())
    check(foo.constructors.single().parameterAnnotations.single())
    return "OK"
}

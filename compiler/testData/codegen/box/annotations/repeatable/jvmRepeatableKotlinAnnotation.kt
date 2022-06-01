// !LANGUAGE: +RepeatableAnnotations
// !API_VERSION: LATEST
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8
// STDLIB_JDK8

// java.lang.NoSuchMethodError: java.lang.Class.getAnnotationsByType
// IGNORE_BACKEND: ANDROID

// In light analysis mode, repeated annotations are not wrapped into the container. This is by design, so that in kapt stubs repeated
// annotations will be visible unwrapped.
// IGNORE_LIGHT_ANALYSIS

@JvmRepeatable(As::class)
annotation class A(val value: String)

annotation class As(val value: Array<A>)

@A("O")
@A("")
@A("K")
class Z

fun box(): String {
    val annotations = Z::class.java.annotations.filter { it.annotationClass != Metadata::class }
    val aa = annotations.singleOrNull() ?: return "Fail 1: $annotations"
    if (aa !is As) return "Fail 2: $aa"

    val a = aa.value.asList()
    if (a.size != 3) return "Fail 3: $a"

    val bytype = Z::class.java.getAnnotationsByType(A::class.java)
    if (a.toList() != bytype.toList()) return "Fail 4: ${a.toList()} != ${bytype.toList()}"

    return a.fold("") { acc, it -> acc + it.value }
}

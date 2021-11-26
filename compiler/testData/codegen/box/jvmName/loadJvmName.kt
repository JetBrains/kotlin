// TARGET_BACKEND: JVM
// WITH_STDLIB

@JvmName("bar")
fun foo() {}

fun getJvmName(): JvmName? =
        Class.forName("LoadJvmNameKt").declaredMethods.single { it.name == "bar" }.getAnnotation(JvmName::class.java)

fun box(): String {
    // JvmName is binary-retained and should not be accessible via reflection
    return if (getJvmName() == null) "OK" else "Fail"
}

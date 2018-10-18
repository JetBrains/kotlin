// SKIP_JDK6
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
// JAVAC_OPTIONS: -parameters
// KOTLIN_CONFIGURATION_FLAGS: +JVM.PARAMETERS_METADATA

// FILE: JavaInterface.java

public interface JavaInterface {
    void plugin(String id);
}

// FILE: test.kt

import kotlin.test.assertEquals

interface KotlinInterface {
    fun plugin(id: String)
}

class KotlinDelegate(impl: KotlinInterface) : KotlinInterface by impl

class JavaDelegate(impl: JavaInterface) : JavaInterface by impl

private fun check(javaClass: Class<*>) {
    val pluginMethod = javaClass.getDeclaredMethod("plugin", String::class.java)
    assertEquals(listOf("id"), pluginMethod.parameters.map { it.name }, "Incorrect parameters for $javaClass")
}

fun box(): String {
    check(JavaInterface::class.java)
    check(KotlinInterface::class.java)
    check(KotlinDelegate::class.java)
    check(JavaDelegate::class.java)
    return "OK"
}

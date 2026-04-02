// TARGET_BACKEND: JVM
// WITH_REFLECT
import kotlin.jvm.internal.*
import kotlin.test.assertEquals

fun box(): String {
    // Simulate the case of a property that does not exist (e.g., has been removed by Proguard), and check that calling `name` doesn't
    // lead to the initialization of full reflection.

    val owner = Reflection.getOrCreateKotlinPackage(object {}::class.java.enclosingClass, "")
    val name = "unresolved"
    val signature = "getUnresolved()Ljava/lang/String;"

    assertEquals(name, Reflection.property0(PropertyReference0Impl(owner, name, signature)).name)
    assertEquals(name, Reflection.mutableProperty0(MutablePropertyReference0Impl(owner, name, signature)).name)
    assertEquals(name, Reflection.property1(PropertyReference1Impl(owner, name, signature)).name)
    assertEquals(name, Reflection.mutableProperty1(MutablePropertyReference1Impl(owner, name, signature)).name)
    assertEquals(name, Reflection.property2(PropertyReference2Impl(owner, name, signature)).name)
    assertEquals(name, Reflection.mutableProperty2(MutablePropertyReference2Impl(owner, name, signature)).name)

    return "OK"
}

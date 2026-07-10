// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: CallableAnnotation.java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CallableAnnotation {
    String name()    default "default-name";
    int    version() default 1;
    boolean enabled() default true;
}

// FILE: box.kt
// Tests that Java annotation element accessor properties are callable via reflection,
// including via KProperty0.getter.call() and KProperty0.call().

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*
import kotlin.test.*

fun box(): String {
    val annClass = CallableAnnotation::class

    // Static properties represent annotation elements
    val staticProps = annClass.staticProperties
    assertTrue(staticProps.isNotEmpty(),
        "CallableAnnotation must have staticProperties for its elements, got: ${staticProps.map { it.name }}")

    val nameProp   = staticProps.firstOrNull { it.name == "name" }
    val versionProp = staticProps.firstOrNull { it.name == "version" }
    val enabledProp = staticProps.firstOrNull { it.name == "enabled" }

    // Check the properties exist
    assertNotNull(nameProp,    "CallableAnnotation must have 'name' static property")
    assertNotNull(versionProp, "CallableAnnotation must have 'version' static property")
    assertNotNull(enabledProp, "CallableAnnotation must have 'enabled' static property")

    // Call the default value accessors — they must not throw
    val nameDefault    = runCatching { nameProp.call() }
        .getOrElse { return "Fail: calling 'name' threw ${it::class.simpleName}: ${it.message}" }
    val versionDefault = runCatching { versionProp.call() }
        .getOrElse { return "Fail: calling 'version' threw ${it::class.simpleName}: ${it.message}" }
    val enabledDefault = runCatching { enabledProp.call() }
        .getOrElse { return "Fail: calling 'enabled' threw ${it::class.simpleName}: ${it.message}" }

    assertEquals("default-name", nameDefault,    "name default should be 'default-name'")
    assertEquals(1,               versionDefault, "version default should be 1")
    assertEquals(true,            enabledDefault, "enabled default should be true")

    // Also verify via getter.call()
    val nameViaGetter = runCatching { nameProp.getter.call() }
        .getOrElse { return "Fail: name getter.call() threw ${it::class.simpleName}: ${it.message}" }
    assertEquals("default-name", nameViaGetter)

    return "OK"
}

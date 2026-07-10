// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocksAndExtensions

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*
import kotlin.test.*

class WithJvmStaticInCompanionBlock {
    companion {
        // @JvmStatic has no effect inside companion blocks (members are already static),
        // but the annotation should still be reflected on the callable.
        @JvmStatic
        fun alreadyStatic(): String = "static"

        // Without @JvmStatic for comparison
        fun plainStatic(): String = "plain"

        @JvmStatic
        val staticProp: Int = 42
    }
}

class WithJvmStaticInCompanionObject(val x: Int) {
    companion object {
        @JvmStatic
        fun fromObject(): String = "from-object"

        fun notJvmStatic(): String = "not-static"
    }
}

fun box(): String {
    // Companion block: both @JvmStatic and plain functions appear in staticFunctions
    val blockFns = WithJvmStaticInCompanionBlock::class.staticFunctions.associateBy { it.name }
    assertNotNull(blockFns["alreadyStatic"], "alreadyStatic should be in staticFunctions")
    assertNotNull(blockFns["plainStatic"], "plainStatic should be in staticFunctions")

    // @JvmStatic annotation is reflected on the callable
    val annotated = blockFns["alreadyStatic"]!!
    val jvmStaticAnn = annotated.annotations.firstOrNull { it.annotationClass.simpleName == "JvmStatic" }
    assertNotNull(jvmStaticAnn, "@JvmStatic should appear in annotations of the function")

    // Plain companion block function has no @JvmStatic annotation
    val plain = blockFns["plainStatic"]!!
    val plainJvmStatic = plain.annotations.firstOrNull { it.annotationClass.simpleName == "JvmStatic" }
    assertNull(plainJvmStatic, "plainStatic should not have @JvmStatic annotation")

    // Both are callable without an instance (static — no INSTANCE parameter)
    assertEquals(emptyList(), annotated.parameters.filter { it.kind == KParameter.Kind.INSTANCE })
    assertEquals(emptyList(), plain.parameters.filter { it.kind == KParameter.Kind.INSTANCE })
    assertEquals("static", annotated.call())
    assertEquals("plain", plain.call())

    // @JvmStatic property in companion block
    val staticProp = WithJvmStaticInCompanionBlock::class.staticProperties.firstOrNull { it.name == "staticProp" }
    assertNotNull(staticProp, "staticProp should be in staticProperties")
    assertEquals(42, staticProp.call())
    val propJvmStatic = staticProp.annotations.firstOrNull { it.annotationClass.simpleName == "JvmStatic" }
    assertNotNull(propJvmStatic, "@JvmStatic should appear in property annotations")

    // Companion OBJECT with @JvmStatic: function appears both as static and as member of companion
    // The @JvmStatic function is in staticFunctions of the outer class
    val objStaticFns = WithJvmStaticInCompanionObject::class.staticFunctions.map { it.name }.toSet()
    assertTrue("fromObject" in objStaticFns,
        "@JvmStatic in companion object should appear in outer class staticFunctions: $objStaticFns")
    assertFalse("notJvmStatic" in objStaticFns,
        "Non-@JvmStatic companion function should NOT appear in outer class staticFunctions: $objStaticFns")

    // Verify javaMethod is accessible for the @JvmStatic companion block function
    val javaMethod = annotated.javaMethod
    assertNotNull(javaMethod, "javaMethod should be accessible for @JvmStatic companion block function")
    assertEquals("alreadyStatic", javaMethod.name)

    return "OK"
}

// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocksAndExtensions
// Tests that companion extensions declared at top-level are visible as static members
// of the file facade class via reflection.

import kotlin.reflect.full.*
import kotlin.test.*

class Widget(val id: Int) {
    companion {
        val defaultId: Int = 0
        fun create(): Widget = Widget(defaultId)
    }
}

companion val Widget.label: String get() = "widget-${Widget.defaultId}"
companion fun Widget.describe(): String = "Widget[default=${Widget.defaultId}]"
companion fun Widget.withId(n: Int): Widget = Widget(n)

fun box(): String {
    // Companion BLOCK members are on Widget::class as staticFunctions/staticProperties
    val widgetStatic = Widget::class.staticFunctions.map { it.name }.toSet()
    assertTrue("create" in widgetStatic, "create should be in Widget::class.staticFunctions")

    // Companion EXTENSIONS are on the file facade class
    val facadeKlass = Class.forName("CompanionExtensionStaticVisibilityKt").kotlin

    val facadeStaticFns = facadeKlass.staticFunctions.map { it.name }.toSet()
    assertTrue(facadeStaticFns.isNotEmpty(),
        "File facade must have static functions for companion extensions, got: $facadeStaticFns")
    assertTrue("describe" in facadeStaticFns,
        "describe() companion extension must be in facade staticFunctions, got: $facadeStaticFns")
    assertTrue("withId" in facadeStaticFns,
        "withId() companion extension must be in facade staticFunctions, got: $facadeStaticFns")

    val facadeStaticProps = facadeKlass.staticProperties.map { it.name }.toSet()
    assertTrue("label" in facadeStaticProps,
        "label companion extension property must be in facade staticProperties, got: $facadeStaticProps")

    // Companion extensions are callable via the facade
    val describeFn = facadeKlass.staticFunctions.first { it.name == "describe" }
    assertEquals("Widget[default=0]", describeFn.call())

    val withIdFn = facadeKlass.staticFunctions.first { it.name == "withId" }
    val w = withIdFn.call(42) as Widget
    assertEquals(42, w.id)

    return "OK"
}

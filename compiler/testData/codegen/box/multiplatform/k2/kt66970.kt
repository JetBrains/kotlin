// ISSUE: KT-66970
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib-common
// FILE: lib-common.kt
object JvmStaticTest {
    @kotlin.jvm.JvmStatic
    fun annotatedFunction(): String = "JvmStatic function"

    @kotlin.jvm.JvmStatic
    val annotatedProperty: String = "JvmStatic property"

    val annotatedGetter: String
        @kotlin.jvm.JvmStatic get() = "JvmStatic property getter"

    var annotatedSetter: String = "JvmStaticTest.annotatedSetter default value"
        get() = field
        @kotlin.jvm.JvmStatic set(newValue) {
            field = newValue
        }
}

// @JsStatic is not supported on regular objects
class JsStaticTest {
    companion object {
        @kotlin.js.JsStatic
        fun annotatedFunction(): String = "JsStatic function"

        @kotlin.js.JsStatic
        val annotatedProperty: String = "JsStatic property"

        val annotatedGetter: String
            @kotlin.js.JsStatic get() = "JsStatic property getter"

        var annotatedSetter: String = "JsStaticTest.Companion.annotatedSetter default value"
            get() = field
            @kotlin.js.JsStatic set(newValue) {
                field = newValue
            }
    }
}

// MODULE: lib()()(lib-common)
// FILE: lib.kt

// Empty platform module so that the main module KLIB had something to link against, since a common "module" can't be compiled to
// a KLIB.

// MODULE: main(lib)
// FILE: main.kt
import kotlin.test.assertEquals

fun box(): String {
    assertEquals(JvmStaticTest.annotatedFunction(), "JvmStatic function")
    assertEquals(JvmStaticTest.annotatedProperty, "JvmStatic property")
    assertEquals(JvmStaticTest.annotatedGetter, "JvmStatic property getter")
    JvmStaticTest.annotatedSetter = "JvmStatic property setter"
    assertEquals(JvmStaticTest.annotatedSetter, "JvmStatic property setter")

    assertEquals(JsStaticTest.annotatedFunction(), "JsStatic function")
    assertEquals(JsStaticTest.annotatedProperty, "JsStatic property")
    assertEquals(JsStaticTest.annotatedGetter, "JsStatic property getter")
    JsStaticTest.annotatedSetter = "JsStatic property setter"
    assertEquals(JsStaticTest.annotatedSetter, "JsStatic property setter")

    return "OK"
}

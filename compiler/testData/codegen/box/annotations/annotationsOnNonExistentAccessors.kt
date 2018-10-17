// WITH_REFLECT
// IGNORE_BACKEND: JVM_IR, JS_IR, JS, NATIVE

// Please make sure that this test is consistent with the diagnostic test "annotationsTargetingNonExistentAccessor.kt"

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KProperty

annotation class Ann
annotation class AnnRepeat

fun check(element: KAnnotatedElement, annotationExists: Boolean) {
    require(element.annotations.isNotEmpty() == annotationExists) { "Fail: $element" }
}

class PrivateProperties(
    @get:Ann private val y0: Int,
    @get:Ann private vararg val y1: String
) {
    @get:Ann
    private val x1 = ""

    @set:Ann
    private var x2 = ""

    @setparam:Ann
    private var x3 = ""

    @setparam:[Ann AnnRepeat]
    private var x4 = ""

    @get:Ann
    internal val x5 = ""

    @get:Ann
    protected val x6 = ""

    @get:Ann
    private val x7: String = ""
        @AnnRepeat get

    @get:Ann
    @set:Ann
    private var x8: String = ""
        get() { return "" }

    @get:Ann
    @set:Ann
    private var x9: String = ""
        get() { return "" }
        set(f) { field = f }

    fun test() {
        check(::y0.getter, annotationExists = false)
        check(::y1.getter, annotationExists = false)
        check(::x1.getter, annotationExists = false)
        check(::x2.setter, annotationExists = false)
        check(::x3.setter.parameters.first(), annotationExists = false)
        check(::x4.setter.parameters.first(), annotationExists = false)

        check(::x5.getter, annotationExists = true)
        check(::x6.getter, annotationExists = true)

        check(::x7.getter, annotationExists = false)

        check(::x8.getter, annotationExists = true)
        check(::x8.setter, annotationExists = false)

        check(::x9.getter, annotationExists = true)
        check(::x9.setter, annotationExists = true)
    }
}

private class EffetivelyPrivate private constructor(
    @get:Ann val x0: Int,
    @get:Ann protected val x1: Int,
    @get:Ann internal val x2: Int
) {
    companion object {
        fun test() {
            EffetivelyPrivate(0, 0, 0).test()
        }
    }

    private class Nested {
        @get:Ann
        val fofo = 0
    }

    fun test() {
        check(::x0.getter, annotationExists = true)
        check(::x1.getter, annotationExists = true)
        check(::x2.getter, annotationExists = true)

        check(Nested::fofo.getter, annotationExists = true)
    }
}

class Statics {
    @get:Ann
    lateinit var y0: String

    @get:Ann
    private lateinit var y1: String

    companion object {
        @JvmField
        @get:Ann
        val x0 = ""

        @get:Ann
        const val x1 = ""

        @JvmStatic
        @AnnRepeat
        @get:Ann
        @set:Ann
        @setparam:Ann
        var x2 = ""

        @JvmStatic
        @get:Ann
        private val x3 = ""

        @get:Ann
        val x4 = ""
    }

    fun test() {
        check(::y0.getter, annotationExists = true)
        check(::y1.getter, annotationExists = false)

        check(::x0.getter, annotationExists = false)
        check(::x1.getter, annotationExists = false)

        check(::x2.getter, annotationExists = true)
        check(::x2.setter, annotationExists = true)
        check(::x2.setter.parameters.first(), annotationExists = true)

        check(::x3.getter, annotationExists = false)

        check(::x4.getter, annotationExists = true)
    }
}

class Delegate {
    @get:Ann
    @set:Ann
    @setparam:Ann
    private var delegate by CustomDelegate()

    fun test() {
        check(::delegate.getter, annotationExists = true)
        check(::delegate.setter, annotationExists = true)
        check(::delegate.setter.parameters.first(), annotationExists = true)
    }

    class CustomDelegate {
        operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
        operator fun setValue(delegate: Delegate, property: KProperty<*>, s: String) {
        }
    }
}

fun box(): String {
    PrivateProperties(0, "").test()
    EffetivelyPrivate.test()
    Statics().test()
    Delegate().test()
    return "OK"
}
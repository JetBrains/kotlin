// Please make sure that this test is consistent with the blackbox test "annotationsOnNonExistentAccessors.kt"

import kotlin.reflect.KProperty

annotation class Ann
annotation class AnnRepeat

class Foo(
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
}

private class EffetivelyPrivate private constructor(
    @get:Ann val x0: Int,
    @get:Ann protected val x1: Int,
    @get:Ann internal val x2: Int
) {
    private class Nested {
        @get:Ann
        val fofo = 0
    }
}

class PrivateToThis<in I> {
    @get:Ann
    @set:Ann
    @setparam:Ann
    private var x0: I = TODO()
}

class Statics {
    companion object {
        @JvmField
        @get:Ann
        val x0 = ""

        @get:Ann
        const val x1 = ""

        @JvmStatic
        @get:Ann
        val x2 = ""

        @JvmStatic
        @get:Ann
        private val x3 = ""

        @JvmStatic
        @get:Ann
        private val x4 = ""
    }
}

private class Other(@param:Ann private val param: Int) {
    @property:Ann
    @field:Ann
    private val other = ""

    private fun @receiver:Ann Int.receiver() {}

    @delegate:Ann
    @get:Ann
    private val delegate by CustomDelegate()
}

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnn

class WithSource {
    @get:SourceAnn
    @set:SourceAnn
    @setparam:SourceAnn
    private var x0 = ""

    private val x1 = ""
        @SourceAnn get
}

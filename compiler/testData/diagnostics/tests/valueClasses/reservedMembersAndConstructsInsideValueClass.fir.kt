// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class IC1(val x: Any) {
    fun box() {}
    fun box(x: Any) {}

    fun unbox() {}
    fun unbox(x: Any) {}

    override fun equals(other: Any?): Boolean = true
    override fun hashCode(): Int = 0
}

@JvmInline
value class IC2(val x: Any) {
    fun box(x: Any) {}
    fun box(): Any = TODO()

    fun unbox(x: Any) {}
    fun unbox(): Any = TODO()

    fun equals(my: Any, other: Any): Boolean = true
    fun hashCode(a: Any): Int = 0
}

@JvmInline
value class IC3(val x: Any) {
    fun box(x: Any): Any = TODO()
    fun unbox(x: Any): Any = TODO()

    fun equals(): Boolean = true
}

interface WithBox {
    fun box(): String
}

@JvmInline
value class IC4(val s: String) : WithBox {
    override fun box(): String = ""
}

@JvmInline
value class IC5(val a: String) {
    constructor(i: Int) : this(i.toString()) {
        TODO("something")
    }
}
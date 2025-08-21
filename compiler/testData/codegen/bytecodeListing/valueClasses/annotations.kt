// LANGUAGE: +ValueClasses
// WITH_STDLIB
// FIR_IDENTICAL
// TARGET_BACKEND: JVM_IR
// JVM_ABI_K1_K2_DIFF: KT-62582, KT-69075

import kotlin.reflect.KProperty


@Repeatable
annotation class Ann

@[Ann Ann]
@JvmInline
value class A @Ann constructor(
    @[Ann Ann]
    @param:[Ann Ann]
    @property:[Ann Ann]
    @field:[Ann Ann]
    @get:[Ann Ann]
    val x: Int,
    @[Ann Ann]
    @param:[Ann Ann]
    @property:[Ann Ann]
    @field:[Ann Ann]
    @get:[Ann Ann]
    val y: Int,
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return 0
    }
}

@[Ann Ann]
@JvmInline
value class B @Ann constructor(
    @property:[Ann Ann]
    val x: A,
    @[Ann Ann]
    @param:[Ann Ann]
    @property:[Ann Ann]
    @field:[Ann Ann]
    @get:[Ann Ann]
    val y: A?,
)

@[Ann Ann]
class C @Ann constructor(
    @property:[Ann Ann]
    @set:[Ann Ann]
    var x: A,
    @[Ann Ann]
    @param:[Ann Ann]
    @property:[Ann Ann]
    @field:[Ann Ann]
    @get:[Ann Ann]
    @set:[Ann Ann]
    @setparam:[Ann Ann]
    var y: A?,
) {
    @delegate:[Ann Ann]
    @property:[Ann Ann]
    val z by lazy { A(-100, -200) }
    @property:[Ann Ann]
    @get:[Ann Ann]
    val t by A(-100, -200)
    @property:[Ann Ann]
    val d by ::z

    init {
        if (2 + 2 == 4) {
            @[Ann Ann]
            val x = 4
            val y = A(1, 2)
        }


        fun f() {
            if (2 + 2 == 4) {
                @[Ann Ann]
                val x = 4
                val y = A(1, 2)
            }
        }
    }
}


@[Ann Ann]
fun A.t(a: A, b: B, @[Ann Ann] c: C) {
    if (2 + 2 == 4) {
        @[Ann Ann]
        val x = 4
        val y = A(1, 2)
    }

    fun f() {
        if (2 + 2 == 4) {
            @[Ann Ann]
            val x1 = 4
            val y1 = A(1, 2)
        }
    }
}

@[Ann Ann]
fun @receiver:[Ann Ann] C.t(a: A, b: B, @[Ann Ann] c: C) = 4

@[Ann Ann]
var A.t
    @[Ann Ann]
    get() = A(1, 2)
    @[Ann Ann]
    set(_) = Unit

@[Ann Ann]
var @receiver:[Ann Ann] C.t
    @[Ann Ann]
    get() = A(1, 2)
    @[Ann Ann]
    set(_) = Unit

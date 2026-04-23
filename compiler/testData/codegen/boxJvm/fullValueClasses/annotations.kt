// LANGUAGE: +FullValueClasses, -PropertyParamAnnotationDefaultTargetMode
// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
// WITH_REFLECT

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@Repeatable
annotation class Ann

abstract value class Sealed
abstract value class Base: Sealed()

@[Ann Ann]
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
): Base() {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return 0
    }
}

@[Ann Ann]
value class B @Ann constructor(
    @[Ann Ann]
    @param:[Ann Ann]
    @property:[Ann Ann]
    @field:[Ann Ann]
    @get:[Ann Ann]
    val x: A,
    @[Ann Ann]
    @param:[Ann Ann]
    @property:[Ann Ann]
    @field:[Ann Ann]
    @get:[Ann Ann]
    val y: A?,
) {
    @JvmName("otherName")
    fun f() = Unit
}

typealias NullableA = A?

@[Ann Ann]
class C @Ann constructor(
    @[Ann Ann]
    @param:[Ann Ann]
    @property:[Ann Ann]
    @field:[Ann Ann]
    @get:[Ann Ann]
    @set:[Ann Ann]
    @setparam:[Ann Ann]
    var x: A,
    @[Ann Ann]
    @param:[Ann Ann]
    @property:[Ann Ann]
    @field:[Ann Ann]
    @get:[Ann Ann]
    @set:[Ann Ann]
    @setparam:[Ann Ann]
    var y: A?,
    @[Ann Ann]
    @param:[Ann Ann]
    @property:[Ann Ann]
    @field:[Ann Ann]
    @get:[Ann Ann]
    @set:[Ann Ann]
    @setparam:[Ann Ann]
    var yTa: NullableA,
) {
    @delegate:[Ann Ann]
    @property:[Ann Ann]
    @get:[Ann Ann]
    val z by lazy { A(-100, -200) }
    @delegate:[Ann Ann]
    @property:[Ann Ann]
    @get:[Ann Ann]
    val c by A(-100, -200)
    @delegate:[Ann Ann]
    @property:[Ann Ann]
    @get:[Ann Ann]
    val d by ::z
    
    @JvmField
    val e = x
    
    init {
        if (2 + 2 == 4) {
            @[Ann Ann]
            val x = 4
            @[Ann Ann]
            val y = A(1, 2)
        }
        
        
        fun f() {
            if (2 + 2 == 4) {
                @[Ann Ann]
                val x = 4
                @[Ann Ann]
                val y = A(1, 2)
            }
        }
    }
}


@[Ann Ann]
fun @receiver:[Ann Ann] A.t(@[Ann Ann] a: A, @[Ann Ann] b: B, @[Ann Ann] c: C) {
    if (2 + 2 == 4) {
        @[Ann Ann]
        val x = 4
        @[Ann Ann]
        val y = A(1, 2)
    }

    fun f() {
        if (2 + 2 == 4) {
            @[Ann Ann]
            val x1 = 4
            @[Ann Ann]
            val y1 = A(1, 2)
        }
    }
}

@[Ann Ann]
fun @receiver:[Ann Ann] C.t(@[Ann Ann] a: A, @[Ann Ann] b: B, @[Ann Ann] c: C) = 4

@[Ann Ann]
var @receiver:[Ann Ann] A.tP
    @[Ann Ann]
    get() = A(1, 2)
    @[Ann Ann]
    set(@[Ann Ann] _) = Unit

@[Ann Ann]
var @receiver:[Ann Ann] C.tP
    @[Ann Ann]
    get() = A(1, 2)
    @[Ann Ann]
    set(@[Ann Ann] _) = Unit

fun box(): String {
    if (!A::class.isValue) return "Failed: A class should be a value class"
    if (!B::class.isValue) return "Failed: B class should be a value class"
    if (C::class.isValue) return "Failed: C class should be not a value class"

    // Check class annotations
    if (A::class.annotations.count { it is Ann } != 2) return "Failed: A class should have 2 @Ann"
    if (B::class.annotations.count { it is Ann } != 2) return "Failed: B class should have 2 @Ann"
    if (C::class.annotations.count { it is Ann } != 2) return "Failed: C class should have 2 @Ann"

    // Check constructor annotations
    if (A::class.constructors.first().annotations.count { it is Ann } != 1) return "Failed: A constructor @Ann"
    if (B::class.constructors.first().annotations.count { it is Ann } != 1) return "Failed: B constructor @Ann"
    if (C::class.constructors.first().annotations.count { it is Ann } != 1) return "Failed: C constructor @Ann"

    // Check property annotations for A
    val aProps = A::class.members.filter { it.name in setOf("x", "y") }
    for (prop in aProps) {
        if (prop.annotations.count { it is Ann } != 2) return "Failed: A property ${prop.name} @Ann count"
    }

    // Check B's properties
    val bProps = B::class.members.filter { it.name in setOf("x", "y") }
    for (prop in bProps) {
        if (prop.annotations.count { it is Ann } != 2) return "Failed: B property ${prop.name} @Ann count"
    }

    // Check C's properties including delegates
    val cProps = C::class.members.filter { it.name in setOf("x", "y", "yTa", "z", "c", "d") }
    for (prop in cProps) {
        if (prop.annotations.count { it is Ann } < 2) return "Failed: C property ${prop.name} @Ann count"
    }

    // Check extension function annotations
    if (A::tP.annotations.count { it is Ann } != 2) return "Failed: A.t extension @Ann count"

    if (C::tP.annotations.count { it is Ann } != 2) return "Failed: C.t extension @Ann count"
    fun KClass<*>.superClass() = supertypes.single().classifier as KClass<*>

    if (!(A::class.superClass().isValue)) return "Failed: Base is not a value class"
    if (!(A::class.superClass().superClass().isValue)) return "Failed: Sealed is not a value class"

    return "OK"
}

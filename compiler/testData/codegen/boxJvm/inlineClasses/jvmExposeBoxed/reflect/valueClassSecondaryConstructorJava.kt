// WITH_REFLECT
// TARGET_BACKEND: JVM

// KT-89068: the exposed secondary constructor is visible to Java reflection and usable from it.

@file:OptIn(ExperimentalStdlibApi::class)

package test

@JvmInline
value class Exposed(val a: UInt) {
    @JvmExposeBoxed
    constructor(param1: UInt, param2: String) : this(param1 + param2.length.toUInt())
}

@JvmInline
value class NonExposed(val a: UInt) {
    constructor(param1: UInt, param2: String) : this(param1 + param2.length.toUInt())
}

fun box(): String {
    val exposedCtors = Exposed::class.java.declaredConstructors.map { it.toString() }.sorted()
    if (exposedCtors != listOf("private test.Exposed(int)", "public test.Exposed(kotlin.UInt,java.lang.String)")) {
        return "FAIL 1: $exposedCtors"
    }

    // Without the annotation nothing is added.
    val nonExposedCtors = NonExposed::class.java.declaredConstructors.map { it.toString() }.sorted()
    if (nonExposedCtors != listOf("private test.NonExposed(int)")) return "FAIL 2: $nonExposedCtors"

    // @JvmExposeBoxed is not runtime-visible, so it is checked by the bytecode listing test instead.
    // What matters here is that the constructor really runs the secondary constructor.
    val uintClass = Class.forName("kotlin.UInt")
    val ctor = Exposed::class.java.getDeclaredConstructor(uintClass, String::class.java)
    val instance = ctor.newInstance(42u, "OK") as Exposed
    if (instance.a != 44u) return "FAIL 3: ${instance.a}"

    return "OK"
}

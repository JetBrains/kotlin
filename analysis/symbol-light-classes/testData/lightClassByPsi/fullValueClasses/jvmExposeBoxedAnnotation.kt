// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

package pack

@JvmExposeBoxed
value class ValueClass(val first: String, val second: Int) {
    fun funWithSelfParameter(v: ValueClass) {}
    val propertyWithValueClassType: ValueClass get() = this
}

class Regular {
    @JvmExposeBoxed
    fun function(v: ValueClass): ValueClass = v

    @JvmExposeBoxed("exposedName")
    fun functionWithName(v: ValueClass): ValueClass = v

    @JvmExposeBoxed("exposedName2")
    @JvmName("jvmName")
    fun functionWithJvmName(v: ValueClass): ValueClass = v

    @get:JvmExposeBoxed
    @set:JvmExposeBoxed
    var property: ValueClass? = null
}

@JvmExposeBoxed
fun topLevelFunction(v: ValueClass): ValueClass = v

// DECLARATIONS_NO_LIGHT_ELEMENTS: Regular.class[functionWithName]
// LIGHT_ELEMENTS_NO_DECLARATION: Regular.class[exposedName]

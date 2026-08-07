// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IC(val s: String)

class SyntheticHost {
    @JvmSynthetic
    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SYNTHETIC!>@JvmExposeBoxed<!>
    fun member(ic: IC) {}

    @get:JvmSynthetic
    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SYNTHETIC!>@get:JvmExposeBoxed<!>
    val getterOnly: IC = TODO()
}

// The value class parameter is there on purpose, so that the diagnostic is not masked by
// USELESS_JVM_EXPOSE_BOXED as it is in 'simple.kt'.
fun withLocal() {
    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_LOCALS!>@JvmExposeBoxed<!>
    fun local(ic: IC): IC = ic

    local(IC(""))
}

// A member of a local class is not itself declared in a block, and the local class does have a real JVM name,
// so the member is exposable and nothing is reported.
fun withLocalClass() {
    class Local {
        @JvmExposeBoxed
        fun member(ic: IC): IC = ic
    }

    Local().member(IC(""))
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, annotationUseSiteTargetPropertyGetter, classDeclaration,
classReference, functionDeclaration, localClass, localFunction, primaryConstructor, propertyDeclaration, stringLiteral,
value */

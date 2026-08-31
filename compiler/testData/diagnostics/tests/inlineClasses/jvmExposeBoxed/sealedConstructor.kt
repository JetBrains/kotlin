// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IC(val s: String)

sealed class DirectlyAnnotated <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SEALED_CONSTRUCTOR!>@JvmExposeBoxed<!> constructor(ic: IC) {
    <!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_SEALED_CONSTRUCTOR!>@JvmExposeBoxed<!>
    constructor(ic: IC, i: Int) : this(ic)
}

@JvmExposeBoxed
sealed class Propagated(ic: IC)

abstract class Abstract @JvmExposeBoxed constructor(ic: IC)

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, primaryConstructor,
propertyDeclaration, sealed, secondaryConstructor, value */

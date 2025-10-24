// RUN_PIPELINE_TILL: BACKEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline value class PositiveInt @JvmExposeBoxed constructor (val value: Int) {
    @JvmExposeBoxed fun toInt(): Int = value
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, functionDeclaration,
primaryConstructor, propertyDeclaration, value */

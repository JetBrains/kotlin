// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IC(val s: String)

class E {
    @JvmExposeBoxed(<!JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME!>"same"<!>)
    fun same(ic: IC?): String = ""
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, functionDeclaration, nullableType,
primaryConstructor, propertyDeclaration, stringLiteral, value */

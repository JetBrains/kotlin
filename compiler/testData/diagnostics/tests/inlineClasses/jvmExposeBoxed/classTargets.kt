// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IC(val s: String)

// A 'fun interface' is still an interface: its single abstract member can never be exposed, so the
// class-level annotation is rejected just like it is on a plain interface.
<!JVM_EXPOSE_BOXED_CANNOT_EXPOSE_OPEN_ABSTRACT!>@JvmExposeBoxed<!>
fun interface Transform {
    fun apply(ic: IC): IC
}

// An annotation class can carry a value class after all - unsigned types are valid annotation member types,
// unlike user value classes, which are INVALID_TYPE_OF_ANNOTATION_MEMBER. Annotation methods keep their
// declared name and their erased return type, so they are never mangled and there is nothing to expose;
// neither the class-level annotation nor an explicit accessor request is reported.
@JvmExposeBoxed
annotation class Unsigned(val count: UInt)

annotation class UnsignedAccessor(@get:JvmExposeBoxed val count: UInt)

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetFile, annotationUseSiteTargetPropertyGetter,
classDeclaration, classReference, funInterface, functionDeclaration, interfaceDeclaration, primaryConstructor,
propertyDeclaration, value */

// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-73149
import kotlin.annotation.AnnotationTarget.TYPE

@Target(TYPE)
annotation class AnnotationWithTypeTarget

@Target(TYPE)
annotation class AnnotationWithConstructor(val k: String)

class A

fun annotationOnContextType(a: context(@AnnotationWithTypeTarget A) () -> Unit) { }

fun annotationWithConstructorOnContextType(a: context(@AnnotationWithConstructor("") A) () -> Unit) { }

fun annotationOnTypeWithContext(a: @AnnotationWithTypeTarget context(A) () -> Unit) { }

fun annotationOnFunWithMoreThenOneContextType(a: context(@AnnotationWithTypeTarget A, @AnnotationWithTypeTarget String) () -> Unit) { }

fun annotationOnValueParameterWithContextType(a: context(A) (@AnnotationWithTypeTarget A) -> Unit) { }

fun annotationOnExtensionParameterWithContextType(a: context(A) (@AnnotationWithTypeTarget A).() -> Unit) { }

// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

@DslMarker @Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Marker

class A
class B
class C

fun report1(foo: <!DSL_MARKER_PROPAGATES_TO_MANY!>@Marker<!> context(A) B.() -> Unit) { }
fun report2(foo: <!DSL_MARKER_PROPAGATES_TO_MANY!>@Marker<!> context(A, B) () -> Unit) { }
fun report3(foo: <!DSL_MARKER_PROPAGATES_TO_MANY!>@Marker<!> context(A, B) C.() -> Unit) { }

fun inside1(foo: context(@Marker A) B.() -> Unit) { }
fun inside2(foo: context(@Marker A, B) () -> Unit) { }
fun inside3(foo: context(A, B) (@Marker C).() -> Unit) { }

fun ok1(foo: @Marker () -> Unit) { }
fun ok2(foo: @Marker context(A) () -> Unit) { }
fun ok3(foo: @Marker B.() -> Unit) { }

fun nothing1(foo: context(A) B.() -> Unit) { }
fun nothing2(foo: context(A, B) () -> Unit) { }
fun nothing3(foo: context(A, B) C.() -> Unit) { }

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, functionalType, typeWithContext,
typeWithExtension */

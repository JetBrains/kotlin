// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IC(val s: String)

@JvmExposeBoxed("foo")
@JvmName("foo")
fun bar1(ic: IC) {}

@JvmExposeBoxed(<!JVM_EXPOSE_BOXED_CANNOT_BE_THE_SAME_AS_JVM_NAME!>"foo"<!>)
@JvmName("foo")
fun barIC(): IC = TODO()

@JvmExposeBoxed
@JvmName("foo")
fun bar2(ic: IC): IC = TODO()

<!JVM_EXPOSE_BOXED_REQUIRES_NAME!>@JvmExposeBoxed<!>
@JvmName("foo")
fun barIC2(): IC = TODO()

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, functionDeclaration,
primaryConstructor, propertyDeclaration, stringLiteral, value */

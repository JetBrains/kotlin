// RUN_PIPELINE_TILL: FRONTEND
// SKIP_JAVAC
// WITH_STDLIB

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class IC(val s: String)

@JvmExposeBoxed(<!ILLEGAL_JVM_NAME!>""<!>)
fun emptyName(ic: IC): IC = TODO()

@JvmExposeBoxed(<!ILLEGAL_JVM_NAME!>"<clinit>"<!>)
fun leadingAngle(ic: IC): IC = TODO()

// JVMS 4.2.2 forbids '<' and '>' anywhere in an unqualified name, but 'Name.isValidIdentifier' only rejects a
// leading '<', so the two names below are accepted today and produce a ClassFormatError at class load time.
// TODO: Remove if green after the fix
@JvmExposeBoxed(<!ILLEGAL_JVM_NAME!>">"<!>)
fun trailingAngle(ic: IC): IC = TODO()

@JvmExposeBoxed(<!ILLEGAL_JVM_NAME!>"a<b>c"<!>)
fun angleInside(ic: IC): IC = TODO()

// Legal JVM method names that no Java source can call. Accepted by design - '@JvmName' behaves the same way.
@JvmExposeBoxed("class")
fun javaKeyword(ic: IC): IC = TODO()

@JvmExposeBoxed("has space")
fun hasSpace(ic: IC): IC = TODO()

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, functionDeclaration,
primaryConstructor, propertyDeclaration, stringLiteral, value */

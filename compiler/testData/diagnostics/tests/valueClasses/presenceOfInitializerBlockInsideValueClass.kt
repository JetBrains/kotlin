// RUN_PIPELINE_TILL: BACKEND
// SKIP_JAVAC
// LANGUAGE: +InlineClasses
// ALLOW_KOTLIN_PACKAGE
// DIAGNOSTICS: -UNUSED_VARIABLE

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Foo(val x: Int) {
    init {}

    init {
        val f = 1
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, init, integerLiteral, localProperty, primaryConstructor,
propertyDeclaration, value */

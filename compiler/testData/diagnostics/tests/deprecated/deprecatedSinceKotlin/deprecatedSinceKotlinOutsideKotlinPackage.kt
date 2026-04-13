// RUN_PIPELINE_TILL: FRONTEND
package foo.bar

@Deprecated("")
@<!DEPRECATED_SINCE_KOTLIN_OUTSIDE_KOTLIN_SUBPACKAGE!>DeprecatedSinceKotlin<!>("1.3")
fun test() {}

/* GENERATED_FIR_TAGS: functionDeclaration, stringLiteral */

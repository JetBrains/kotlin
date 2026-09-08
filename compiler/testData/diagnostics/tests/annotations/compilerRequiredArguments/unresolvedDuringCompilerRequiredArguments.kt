// RUN_PIPELINE_TILL: FRONTEND

package myPack

enum class MyTarget { UNKNOWN_TARGET }

enum class MyLevel { UNKNOWN_LEVEL }

@Target(<!ARGUMENT_TYPE_MISMATCH!>MyTarget.<!AMBIGUOUS_ANNOTATION_ARGUMENT!>UNKNOWN_TARGET<!><!>)
annotation class UnresolvedTarget

@Target(<!ARGUMENT_TYPE_MISMATCH!>arrayOf(MyTarget.<!AMBIGUOUS_ANNOTATION_ARGUMENT!>UNKNOWN_TARGET<!>)<!>)
annotation class UnresolvedTargetInArrayOf

@Target(allowedTargets = <!ARGUMENT_TYPE_MISMATCH!>arrayOf(MyTarget.<!AMBIGUOUS_ANNOTATION_ARGUMENT!>UNKNOWN_TARGET<!>)<!>)
annotation class UnresolvedTargetInNamedArrayOf

@Deprecated("", level = <!ARGUMENT_TYPE_MISMATCH!>MyLevel.<!AMBIGUOUS_ANNOTATION_ARGUMENT!>UNKNOWN_LEVEL<!><!>)
fun unresolvedLevel() {}

fun use() {
    <!DEPRECATION!>unresolvedLevel<!>()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, collectionLiteral, enumDeclaration, enumEntry, functionDeclaration,
stringLiteral */

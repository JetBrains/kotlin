// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-60609
// FIR_DUMP
open class Base(x: Int)
open class NoArgs
interface I

annotation class WithMatchingArgument : <!SUPERTYPES_FOR_ANNOTATION_CLASS!>Base(<!TOO_MANY_ARGUMENTS!>1<!>)<!>
annotation class WithMissingArgument : <!SUPERTYPES_FOR_ANNOTATION_CLASS!>Base()<!>
annotation class WithExtraArgument : <!SUPERTYPES_FOR_ANNOTATION_CLASS!>NoArgs(<!TOO_MANY_ARGUMENTS!>1<!>)<!>
annotation class WithMismatchedArgument : <!SUPERTYPES_FOR_ANNOTATION_CLASS!>Base(<!TOO_MANY_ARGUMENTS!>""<!>)<!>
annotation class WithInterface : <!SUPERTYPES_FOR_ANNOTATION_CLASS!>I, Base(<!TOO_MANY_ARGUMENTS!>1<!>)<!>
annotation class WithAny : <!SUPERTYPES_FOR_ANNOTATION_CLASS!>Any()<!>

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, integerLiteral, interfaceDeclaration, primaryConstructor,
stringLiteral */

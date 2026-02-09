// RUN_PIPELINE_TILL: BACKEND
@RequiresOptIn annotation class A
@RequiresOptIn annotation class B

@OptIn(markerClass = [<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>A<!>::class, <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>B<!>::class])
fun foo() {}

@OptIn(*[<!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>A<!>::class, <!OPT_IN_MARKER_CAN_ONLY_BE_USED_AS_ANNOTATION_OR_ARGUMENT_IN_OPT_IN!>B<!>::class])
fun foo2() {}

/* GENERATED_FIR_TAGS: annotationDeclaration, classReference, collectionLiteral, functionDeclaration */

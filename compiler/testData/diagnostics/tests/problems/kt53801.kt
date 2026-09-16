// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-53801

// KT-53801: False positive INCORRECT_LEFT_COMPONENT_OF_INTERSECTION on DNN
class A2<K : Comparable<K & Any>?>

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, nullableType, typeConstraint, typeParameter */

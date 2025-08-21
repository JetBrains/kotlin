// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79076
// LANGUAGE: -FixationEnhancementsIn22

interface RecordId<T, Self : RecordId<T, Self>>

class MyRecordId : RecordId<String, MyRecordId>

class Foo2<T, Id : RecordId<T, Id>>(underlyingColumn: T)

val x = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo2<!>("")

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nullableType, outProjection, primaryConstructor,
propertyDeclaration, starProjection, typeConstraint, typeParameter */

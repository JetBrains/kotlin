// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-79076
// LANGUAGE: +FixationEnhancementsIn22
// DUMP_INFERENCE_LOGS: FIXATION

interface RecordId<Y, Self : RecordId<Y, Self>>

class MyRecordId : RecordId<String, MyRecordId>

class Foo1<T, Id : RecordId<T, Id>>(underlyingColumn: T, factory: (T) -> Id)

val x = Foo1("") { MyRecordId() }

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nullableType, outProjection, primaryConstructor,
propertyDeclaration, starProjection, typeConstraint, typeParameter */

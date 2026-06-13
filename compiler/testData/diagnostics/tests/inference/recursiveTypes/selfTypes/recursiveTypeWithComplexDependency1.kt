// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-79076
// LANGUAGE_FEATURE_TOGGLED: FixationEnhancementsIn22
// LANGUAGE_FEATURE_TOGGLED_IDENTICAL
// DUMP_INFERENCE_LOGS: FIXATION

interface RecordId<Y, Self : RecordId<Y, Self>>

class MyRecordId : RecordId<String, MyRecordId>

class Foo1<T, Id : RecordId<T, Id>>(underlyingColumn: T, factory: (T) -> Id)

val x = Foo1("") { MyRecordId() }

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, nullableType, outProjection, primaryConstructor,
propertyDeclaration, starProjection, typeConstraint, typeParameter */

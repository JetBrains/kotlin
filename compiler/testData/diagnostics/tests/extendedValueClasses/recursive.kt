// LANGUAGE: +ValueClasses
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

value class A(val y: Int, val z: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>A<!>)
value class B(val y: Int, val z: B?)

value class C(val a: C?, val b: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>C<!>, val c: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>C<!>, val d: C?, val e: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>D<!>)

value class D(val a: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>C<!>)

<!INLINE_CLASS_DEPRECATED!>inline<!> class Inline1(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Inline1<!>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class Inline2(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Inline2?<!>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class Inline3(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Inline4<!>)
<!INLINE_CLASS_DEPRECATED!>inline<!> class Inline4(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Inline3?<!>)

@JvmInline
value class Old1(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>New1<!>)

value class New1(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Old1<!>)

@JvmInline
value class Old2(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>New2<!>)

value class New2(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>New2<!>)

@JvmInline
value class Old3(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Old3<!>)

@JvmInline
value class Old4(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Old4_<!>)

@JvmInline
value class Old4_(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Old4<!>)

@JvmInline
value class Old5(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>New5<!>)

value class New5(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Old5<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Old5<!>)

@JvmInline
value class Old6(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>New6<!>)

value class New6(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>New6<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>New6<!>)

value class New7(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>New7_<!>)

value class New7_(val x: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>New7<!>, val y: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>New7<!>)

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, value */

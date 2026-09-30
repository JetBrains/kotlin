// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

value class A(val a: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>B?<!>, val i: Int)

value class B(val b: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>A<!>)

value class E(val e: F?)

value class F(val f: E?, val i: Int)

value class S(val s: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>T?<!>)

value class T(val t: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>S<!>)

value class Nat(val previous: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>Nat?<!>)

inline value class InlineToFull(val f: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>FullFromInline<!>)

value class FullFromInline(val i: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>InlineToFull?<!>, val x: Int)

inline value class InlineToSingle(val s: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>SingleFromInline<!>)

value class SingleFromInline(val i: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>InlineToSingle?<!>)

<!INLINE_CLASS_DEPRECATED!>inline<!> class DeprecatedInlineToSingle(val s: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>SingleFromDeprecatedInline<!>)

value class SingleFromDeprecatedInline(val i: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>DeprecatedInlineToSingle?<!>)

@JvmInline
value class JvmInlineToSingle(val s: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>SingleFromJvmInline<!>)

value class SingleFromJvmInline(val i: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>JvmInlineToSingle?<!>)

inline value class InlineSelf(val s: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>InlineSelf?<!>)

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, value */

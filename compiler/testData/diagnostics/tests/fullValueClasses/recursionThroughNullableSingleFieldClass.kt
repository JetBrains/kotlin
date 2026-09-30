// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

value class A(val a: B?, val i: Int)

value class B(val b: A)

value class E(val e: F?)

value class F(val f: E?, val i: Int)

value class S(val s: T?)

value class T(val t: S)

value class Nat(val previous: Nat?)

inline value class InlineToFull(val f: FullFromInline)

value class FullFromInline(val i: InlineToFull?, val x: Int)

inline value class InlineToSingle(val s: SingleFromInline)

value class SingleFromInline(val i: InlineToSingle?)

<!INLINE_CLASS_DEPRECATED!>inline<!> class DeprecatedInlineToSingle(val s: SingleFromDeprecatedInline)

value class SingleFromDeprecatedInline(val i: DeprecatedInlineToSingle?)

@JvmInline
value class JvmInlineToSingle(val s: SingleFromJvmInline)

value class SingleFromJvmInline(val i: JvmInlineToSingle?)

inline value class InlineSelf(val s: <!VALUE_CLASS_CANNOT_BE_RECURSIVE!>InlineSelf?<!>)

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, value */

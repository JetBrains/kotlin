// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

<!WRONG_MODIFIER_TARGET!>inline<!> value object InlineValueObject

@JvmInline
<!WRONG_MODIFIER_TARGET!>value<!> interface JvmInlineValueInterface

<!WRONG_MODIFIER_TARGET!>inline<!> <!WRONG_MODIFIER_TARGET!>value<!> interface InlineValueInterface

<!JVM_INLINE_ON_VALUE_OBJECT!>@JvmInline<!>
value object JvmInlineValueObject

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
object JvmInlineObject

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
interface JvmInlineInterface

<!REDUNDANT_JVM_INLINE!>@JvmInline<!>
<!WRONG_MODIFIER_TARGET!>inline<!> value object JvmInlineInlineValueObject

/* GENERATED_FIR_TAGS: interfaceDeclaration, objectDeclaration, value */

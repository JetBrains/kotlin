// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
// ALLOW_KOTLIN_PACKAGE

package kotlin.jvm

annotation class JvmInline

@JvmInline
value class Foo(val x: Int)

<!WRONG_MODIFIER_TARGET!>value<!> interface InlineInterface
<!WRONG_MODIFIER_TARGET!>value<!> annotation class InlineAnn
value object InlineObject
<!WRONG_MODIFIER_TARGET!>value<!> enum class InlineEnum

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, enumDeclaration, interfaceDeclaration, objectDeclaration,
primaryConstructor, propertyDeclaration, value */

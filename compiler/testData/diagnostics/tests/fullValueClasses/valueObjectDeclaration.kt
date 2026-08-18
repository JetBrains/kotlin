// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

interface I {
    fun foo(): Int
}

interface J

open class IdentityClass

sealed value class SealedValueClass

val jDelegate = object : J {}

value object ImplInterface : I {
    val x: Int get() = 42
    override fun foo(): Int = x
    fun member(): String = "member"
}

value object ExtendsValueClass : SealedValueClass()

value object ExtendsIdentity : <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>IdentityClass<!>()

<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>value<!> object CloneableObject : Cloneable

value object WithBackingField {
    <!PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS!>val x: Int<!> = 42
}

value object WithDelegatedProperty {
    val x: Int by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>lazy { 42 }<!>
}

value object ByDelegation : <!VALUE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>J<!> by jDelegate

<!JVM_INLINE_WITHOUT_VALUE_CLASS!>@JvmInline<!>
value object JvmInlineObject

/* GENERATED_FIR_TAGS: anonymousObjectExpression, classDeclaration, functionDeclaration, getter, inheritanceDelegation,
integerLiteral, interfaceDeclaration, lambdaLiteral, objectDeclaration, override, propertyDeclaration, propertyDelegate,
sealed, stringLiteral, value */

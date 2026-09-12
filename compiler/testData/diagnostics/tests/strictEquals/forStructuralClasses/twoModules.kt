// LANGUAGE: -StrictEquals +StrictEqualsForStructuralClasses +FullValueClasses
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

// MODULE: m1
// FILE: m1.kt

interface Unrelated
interface Sup

data class DataPlain(val x: Int)
data class DataExplicit(val x: Int) {
    override fun equals(other: Any?): Boolean = other is DataExplicit
}
data class DataSub(val x: Int) : Sup

data object DataObjectPlain
data object DataObjectSub : Sup

open class WithFinalEquals {
    final override fun equals(other: Any?): Boolean = other is WithFinalEquals
}
data object DataObjectNotGenerated : WithFinalEquals()

value class ValuePlain(val x: Int)
value class ValueExplicit(val x: Int) {
    override fun equals(other: Any?): Boolean = other is ValueExplicit
}
value class ValueSub(val x: Int) : Sup

value object ValueObjectPlain
value object ValueObjectExplicit {
    override fun equals(other: Any?): Boolean = other is ValueObjectExplicit
}

abstract value class AbstractValuePlain
abstract value class AbstractValueExplicit {
    final override fun equals(other: Any?): Boolean = other is AbstractValueExplicit
}
value class AbstractValueImpl(val x: Int) : AbstractValuePlain()

@JvmInline
value class InlineValuePlain(val x: Int)

@JvmInline
value class InlineValueSub(val x: Int) : Sup

enum class EnumPlain { X, Y }
enum class EnumSub : Sup { X, Y }

annotation class AnnotationPlain(val x: Int)

// MODULE: m2(m1)
// FILE: m2.kt

fun testValueClasses(vp: ValuePlain, ve: ValueExplicit, vs: ValueSub, u: Unrelated, s: Sup): Boolean {
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>vp != ve<!>) return true
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>vp == u<!>) return false
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>u == ve<!>) return false
    if (s != vs) return false
    if (vs == s) return true
    return true
}

fun testValueObjects(u: Unrelated): Boolean {
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>ValueObjectPlain != ValueObjectExplicit<!>) return true
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>ValueObjectPlain == u<!>) return false
    return true
}

fun testAbstractValueClasses(
    ap: AbstractValuePlain,
    ae: AbstractValueExplicit,
    ai: AbstractValueImpl,
    vp: ValuePlain,
    u: Unrelated,
): Boolean {
    if (ap != ae) return true
    if (ap == u) return false
    if (ap == ai) return true
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>ap == vp<!>) return false
    return true
}

fun testInlineValueClasses(ip: InlineValuePlain, isub: InlineValueSub, vp: ValuePlain, u: Unrelated, s: Sup): Boolean {
    if (<!EQUALITY_NOT_APPLICABLE!>ip != vp<!>) return true
    if (<!EQUALITY_NOT_APPLICABLE!>ip == u<!>) return false
    if (s != isub) return false
    return true
}

fun testEnums(ep: EnumPlain, es: EnumSub, u: Unrelated, s: Sup, dp: DataPlain): Boolean {
    if (<!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>ep != es<!>) return true
    if (<!INCOMPATIBLE_ENUM_COMPARISON_ERROR!>ep == u<!>) return false
    if (s != es) return false
    if (<!EQUALITY_NOT_APPLICABLE!>ep == dp<!>) return false
    return true
}

fun testSilentToday(
    dp: DataPlain,
    de: DataExplicit,
    ds: DataSub,
    ann: AnnotationPlain,
    u: Unrelated,
    s: Sup,
): Boolean {
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>dp != de<!>) return true
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>dp == u<!>) return false
    if (s != ds) return false
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>DataObjectPlain == u<!>) return false
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>DataObjectNotGenerated != DataObjectPlain<!>) return false
    if (s == DataObjectSub) return true
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>ann == dp<!>) return false
    if (<!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>ann == u<!>) return false
    return true
}

fun testAfterSmartCast(vp: ValuePlain, ep: EnumPlain, dp: DataPlain, nAny: Any?) {
    if (vp == nAny) return
    if (nAny is ValueExplicit && <!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>vp == nAny<!>) return
    if (ep == nAny) return
    if (nAny is EnumSub && <!INCOMPATIBLE_ENUM_COMPARISON!>ep == nAny<!>) return
    if (dp == nAny) return
    if (nAny is DataExplicit && <!INCOMPATIBLE_STRUCTURAL_CLASS_COMPARISON!>dp == nAny<!>) return
}

/* GENERATED_FIR_TAGS: andExpression, annotationDeclaration, classDeclaration, data, enumDeclaration, enumEntry,
equalityExpression, functionDeclaration, ifExpression, integerLiteral, interfaceDeclaration, intersectionType,
isExpression, nullableType, objectDeclaration, operator, override, primaryConstructor, propertyDeclaration, smartcast,
value */

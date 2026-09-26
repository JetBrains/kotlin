// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +FullValueClasses
// LANGUAGE_FEATURE_TOGGLED: StabilizeWillBecomeValueRestrictions

@WillBecomeValue
class Final(val x: Int) {
    override fun equals(other: Any?): Boolean = other is Final && other.x == x
    override fun hashCode(): Int = x
    override fun toString(): String = "Final($x)"
}

@WillBecomeValue
abstract class AbstractBase

@WillBecomeValue
sealed class SealedBase

@WillBecomeValue
object Obj {
    override fun toString(): String = "Obj"
}

@WillBecomeValue
data class Data(val x: Int)

@WillBecomeValue
data object DataObj

<!WILL_BECOME_VALUE_NOT_APPLICABLE_WARNING!>@WillBecomeValue<!>
@JvmInline
value class Val(val x: Int)

<!WILL_BECOME_VALUE_NOT_APPLICABLE_WARNING!>@WillBecomeValue<!>
value class FullVal(val x: Int)

abstract value class AbstractFullVal

@WillBecomeValue
data class ChildFullVal(val x: Int): AbstractFullVal()

@WillBecomeValue
abstract class AbstractAlmostFullVal

@WillBecomeValue
data class ChildAlmostFullVal(val x: Int): AbstractAlmostFullVal()

abstract class Abstract

@WillBecomeValue
data class ChildNotFullVal(val x: Int): <!WILL_BECOME_VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES_WARNING!>Abstract<!>()

<!WILL_BECOME_VALUE_NOT_APPLICABLE_WARNING!>@WillBecomeValue<!>
value object ValObj

<!WILL_BECOME_VALUE_NOT_APPLICABLE_WARNING!>@WillBecomeValue<!>
interface I

<!WILL_BECOME_VALUE_NOT_APPLICABLE_WARNING!>@WillBecomeValue<!>
annotation class Anno

<!WILL_BECOME_VALUE_NOT_APPLICABLE_WARNING!>@WillBecomeValue<!>
enum class E { A }

<!WILL_BECOME_VALUE_NOT_APPLICABLE_WARNING!>@WillBecomeValue<!>
open class Open

/* GENERATED_FIR_TAGS: andExpression, annotationDeclaration, classDeclaration, data, enumDeclaration, enumEntry,
equalityExpression, functionDeclaration, interfaceDeclaration, isExpression, nullableType, objectDeclaration, operator,
override, primaryConstructor, propertyDeclaration, sealed, smartcast, value */

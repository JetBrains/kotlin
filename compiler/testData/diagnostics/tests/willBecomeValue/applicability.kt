// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +FullValueClasses

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
object Obj

@WillBecomeValue
data class Data(val x: Int)

@WillBecomeValue
data object DataObj

<!WILL_BECOME_VALUE_NOT_APPLICABLE!>@WillBecomeValue<!>
@JvmInline
value class Val(val x: Int)

<!WILL_BECOME_VALUE_NOT_APPLICABLE!>@WillBecomeValue<!>
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
data class ChildNotFullVal(val x: Int): <!VALUE_CLASS_CANNOT_EXTEND_IDENTITY_CLASSES!>Abstract<!>()

<!WILL_BECOME_VALUE_NOT_APPLICABLE!>@WillBecomeValue<!>
value object ValObj

<!WILL_BECOME_VALUE_NOT_APPLICABLE!>@WillBecomeValue<!>
interface I

<!WILL_BECOME_VALUE_NOT_APPLICABLE!>@WillBecomeValue<!>
annotation class Anno

<!WILL_BECOME_VALUE_NOT_APPLICABLE!>@WillBecomeValue<!>
enum class E { A }

<!WILL_BECOME_VALUE_NOT_APPLICABLE!>@WillBecomeValue<!>
open class Open

/* GENERATED_FIR_TAGS: andExpression, annotationDeclaration, classDeclaration, data, enumDeclaration, enumEntry,
equalityExpression, functionDeclaration, interfaceDeclaration, isExpression, nullableType, objectDeclaration, operator,
override, primaryConstructor, propertyDeclaration, sealed, smartcast, value */

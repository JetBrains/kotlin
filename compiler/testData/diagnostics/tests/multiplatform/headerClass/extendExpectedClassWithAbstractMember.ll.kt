// MODULE: m1-common
// FILE: common.kt

expect abstract class BaseA() {
    abstract fun foo()
}
expect open <!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class BaseAImpl<!>() : BaseA

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class DerivedA1<!> : BaseAImpl()
class DerivedA2 : BaseAImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



expect interface BaseB {
    fun foo()
}
expect open <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class BaseBImpl<!>() : BaseB

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class DerivedB1<!> : BaseBImpl()
class DerivedB2 : BaseBImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



expect interface BaseC {
    fun foo()
}
expect abstract class BaseCImpl() : BaseC

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class DerivedC1<!> : BaseCImpl()
class DerivedC2 : BaseCImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



expect interface BaseD {
    fun foo()
}
abstract class BaseDImpl() : BaseD {
    fun bar() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



expect interface BaseE {
    fun foo()
}
sealed class BaseEImpl() : BaseE {
    fun bar() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



expect interface BaseF {
    fun foo()
}
expect <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class BaseFImpl<!>() : BaseF



expect abstract class BaseG() {
    abstract fun foo()
}
expect open class BaseGImpl() : BaseG {
    override fun foo()
}
class DerivedG1 : BaseGImpl()

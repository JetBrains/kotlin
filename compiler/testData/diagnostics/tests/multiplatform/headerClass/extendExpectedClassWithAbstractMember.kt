// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

expect abstract class <!NO_ACTUAL_FOR_EXPECT!>BaseA<!>() {
    abstract fun foo()
}
expect open class <!NO_ACTUAL_FOR_EXPECT!>BaseAImpl<!>() : BaseA

class DerivedA1 : BaseAImpl()
class DerivedA2 : BaseAImpl() {
    override fun foo() = super.foo()
}



expect interface <!NO_ACTUAL_FOR_EXPECT!>BaseB<!> {
    fun foo()
}
expect open class <!NO_ACTUAL_FOR_EXPECT!>BaseBImpl<!>() : BaseB

class DerivedB1 : BaseBImpl()
class DerivedB2 : BaseBImpl() {
    override fun foo() = super.foo()
}



expect interface <!NO_ACTUAL_FOR_EXPECT!>BaseC<!> {
    fun foo()
}
expect abstract class <!NO_ACTUAL_FOR_EXPECT!>BaseCImpl<!>() : BaseC

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class DerivedC1<!> : BaseCImpl()
class DerivedC2 : BaseCImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



expect interface <!NO_ACTUAL_FOR_EXPECT!>BaseD<!> {
    fun foo()
}
abstract class BaseDImpl() : BaseD {
    fun bar() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



expect interface <!NO_ACTUAL_FOR_EXPECT!>BaseE<!> {
    fun foo()
}
sealed class BaseEImpl() : BaseE {
    fun bar() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



expect interface <!NO_ACTUAL_FOR_EXPECT!>BaseF<!> {
    fun foo()
}
expect class <!NO_ACTUAL_FOR_EXPECT!>BaseFImpl<!>() : BaseF



expect abstract class <!NO_ACTUAL_FOR_EXPECT!>BaseG<!>() {
    abstract fun foo()
}
expect open class <!NO_ACTUAL_FOR_EXPECT!>BaseGImpl<!>() : BaseG {
    override fun foo()
}
class DerivedG1 : BaseGImpl()

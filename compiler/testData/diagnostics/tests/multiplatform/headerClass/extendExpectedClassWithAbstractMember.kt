// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

expect abstract class <!NO_ACTUAL_FOR_EXPECT{JVM}!>BaseA<!>() {
    abstract fun foo()
}
expect open class <!NO_ACTUAL_FOR_EXPECT{JVM}!>BaseAImpl<!>() : BaseA

class DerivedA1 : BaseAImpl()
class DerivedA2 : BaseAImpl() {
    override fun foo() = super.foo()
}



expect interface <!NO_ACTUAL_FOR_EXPECT{JVM}!>BaseB<!> {
    fun foo()
}
expect open class <!NO_ACTUAL_FOR_EXPECT{JVM}!>BaseBImpl<!>() : BaseB

class DerivedB1 : BaseBImpl()
class DerivedB2 : BaseBImpl() {
    override fun foo() = super.foo()
}



expect interface <!NO_ACTUAL_FOR_EXPECT{JVM}!>BaseC<!> {
    fun foo()
}
expect abstract class <!NO_ACTUAL_FOR_EXPECT{JVM}!>BaseCImpl<!>() : BaseC

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED{JVM}!>class DerivedC1<!> : BaseCImpl()
class DerivedC2 : BaseCImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL, ABSTRACT_SUPER_CALL{JVM}!>foo<!>()
}



expect interface <!NO_ACTUAL_FOR_EXPECT{JVM}!>BaseD<!> {
    fun foo()
}
abstract class BaseDImpl() : BaseD {
    fun bar() = super.<!ABSTRACT_SUPER_CALL, ABSTRACT_SUPER_CALL{JVM}!>foo<!>()
}



expect interface <!NO_ACTUAL_FOR_EXPECT{JVM}!>BaseE<!> {
    fun foo()
}
sealed class BaseEImpl() : BaseE {
    fun bar() = super.<!ABSTRACT_SUPER_CALL, ABSTRACT_SUPER_CALL{JVM}!>foo<!>()
}



expect interface <!NO_ACTUAL_FOR_EXPECT{JVM}!>BaseF<!> {
    fun foo()
}
expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>BaseFImpl<!>() : BaseF



expect abstract class <!NO_ACTUAL_FOR_EXPECT{JVM}!>BaseG<!>() {
    abstract fun foo()
}
expect open class <!NO_ACTUAL_FOR_EXPECT{JVM}!>BaseGImpl<!>() : BaseG {
    override fun foo()
}
class DerivedG1 : BaseGImpl()

// MODULE: m1-jvm()()(m1-common)

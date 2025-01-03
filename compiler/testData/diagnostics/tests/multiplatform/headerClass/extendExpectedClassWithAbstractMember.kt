// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

expect abstract class <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseA<!>() {
    abstract fun foo()
}
expect open class <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseAImpl<!>() : BaseA

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>DerivedA1<!> : BaseAImpl()
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>DerivedA2<!> : BaseAImpl() {
    override fun foo() = super.foo()
}



expect interface <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseB<!> {
    fun foo()
}
expect open class <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseBImpl<!>() : BaseB

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>DerivedB1<!> : BaseBImpl()
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>DerivedB2<!> : BaseBImpl() {
    override fun foo() = super.foo()
}



expect interface <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseC<!> {
    fun foo()
}
expect abstract class <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseCImpl<!>() : BaseC

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>DerivedC1<!><!> : BaseCImpl()
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>DerivedC2<!> : BaseCImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL, ABSTRACT_SUPER_CALL{JVM}!>foo<!>()
}



expect interface <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseD<!> {
    fun foo()
}
abstract class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseDImpl<!>() : BaseD {
    fun bar() = super.<!ABSTRACT_SUPER_CALL, ABSTRACT_SUPER_CALL{JVM}!>foo<!>()
}



expect interface <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseE<!> {
    fun foo()
}
sealed class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseEImpl<!>() : BaseE {
    fun bar() = super.<!ABSTRACT_SUPER_CALL, ABSTRACT_SUPER_CALL{JVM}!>foo<!>()
}



expect interface <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseF<!> {
    fun foo()
}
expect class <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseFImpl<!>() : BaseF



expect abstract class <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseG<!>() {
    abstract fun foo()
}
expect open class <!NO_ACTUAL_FOR_EXPECT{JVM}, PACKAGE_OR_CLASSIFIER_REDECLARATION!>BaseGImpl<!>() : BaseG {
    override fun foo()
}
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>DerivedG1<!> : BaseGImpl()

// MODULE: m1-jvm()()(m1-common)

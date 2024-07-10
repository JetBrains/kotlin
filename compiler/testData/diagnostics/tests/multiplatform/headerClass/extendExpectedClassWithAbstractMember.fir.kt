// MODULE: m1-common
// FILE: common.kt

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect abstract class BaseA() {
    abstract fun foo()
}<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect open <!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED{METADATA}!>class BaseAImpl<!>() : BaseA<!>

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED{METADATA}!>class DerivedA1<!> : BaseAImpl()
class DerivedA2 : BaseAImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL, ABSTRACT_SUPER_CALL{METADATA}!>foo<!>()
}



<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect interface BaseB {
    fun foo()
}<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect open <!ABSTRACT_MEMBER_NOT_IMPLEMENTED{METADATA}!>class BaseBImpl<!>() : BaseB<!>

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED, ABSTRACT_MEMBER_NOT_IMPLEMENTED{METADATA}!>class DerivedB1<!> : BaseBImpl()
class DerivedB2 : BaseBImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL, ABSTRACT_SUPER_CALL{METADATA}!>foo<!>()
}



<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect interface BaseC {
    fun foo()
}<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect abstract class BaseCImpl() : BaseC<!>

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED, ABSTRACT_MEMBER_NOT_IMPLEMENTED{METADATA}!>class DerivedC1<!> : BaseCImpl()
class DerivedC2 : BaseCImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL, ABSTRACT_SUPER_CALL{METADATA}!>foo<!>()
}



<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect interface BaseD {
    fun foo()
}<!>
abstract class BaseDImpl() : BaseD {
    fun bar() = super.<!ABSTRACT_SUPER_CALL, ABSTRACT_SUPER_CALL{METADATA}!>foo<!>()
}



<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect interface BaseE {
    fun foo()
}<!>
sealed class BaseEImpl() : BaseE {
    fun bar() = super.<!ABSTRACT_SUPER_CALL, ABSTRACT_SUPER_CALL{METADATA}!>foo<!>()
}



<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect interface BaseF {
    fun foo()
}<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect <!ABSTRACT_MEMBER_NOT_IMPLEMENTED{METADATA}!>class BaseFImpl<!>() : BaseF<!>



<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect abstract class BaseG() {
    abstract fun foo()
}<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect open class BaseGImpl() : BaseG {
    override fun foo()
}<!>
class DerivedG1 : BaseGImpl()

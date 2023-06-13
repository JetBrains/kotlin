// MODULE: m1-common
// FILE: common.kt

<!NO_ACTUAL_FOR_EXPECT!>expect abstract class BaseA() {
    abstract fun foo()
}<!>
<!NO_ACTUAL_FOR_EXPECT!>expect open class BaseAImpl() : BaseA<!>

class DerivedA1 : BaseAImpl()
class DerivedA2 : BaseAImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



<!NO_ACTUAL_FOR_EXPECT!>expect interface BaseB {
    fun foo()
}<!>
<!NO_ACTUAL_FOR_EXPECT!>expect open class BaseBImpl() : BaseB<!>

class DerivedB1 : BaseBImpl()
class DerivedB2 : BaseBImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



<!NO_ACTUAL_FOR_EXPECT!>expect interface BaseC {
    fun foo()
}<!>
<!NO_ACTUAL_FOR_EXPECT!>expect abstract class BaseCImpl() : BaseC<!>

class DerivedC1 : BaseCImpl()
class DerivedC2 : BaseCImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



<!NO_ACTUAL_FOR_EXPECT!>expect interface BaseD {
    fun foo()
}<!>
abstract class BaseDImpl() : BaseD {
    fun bar() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



<!NO_ACTUAL_FOR_EXPECT!>expect interface BaseE {
    fun foo()
}<!>
sealed class BaseEImpl() : BaseE {
    fun bar() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



<!NO_ACTUAL_FOR_EXPECT!>expect interface BaseF {
    fun foo()
}<!>
<!NO_ACTUAL_FOR_EXPECT!>expect class BaseFImpl() : BaseF<!>

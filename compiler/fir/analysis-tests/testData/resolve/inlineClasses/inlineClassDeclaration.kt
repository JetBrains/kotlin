// WITH_EXTENDED_CHECKERS
class A {
    <!INLINE_CLASS_DEPRECATED, VALUE_CLASS_NOT_TOP_LEVEL!>inline<!> inner class B(val x: Int)
    fun foo() {
        <!INLINE_CLASS_DEPRECATED, VALUE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>inline<!> class C(val x: Int)
    }
    inner <!VALUE_CLASS_NOT_TOP_LEVEL, VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class D(val x: Int)
}

<!VALUE_CLASS_NOT_FINAL!>open<!> <!INLINE_CLASS_DEPRECATED!>inline<!> class NotFinalClass1(val x: Int)
<!VALUE_CLASS_NOT_FINAL!>abstract<!> <!INLINE_CLASS_DEPRECATED!>inline<!> class NotFinalClass2(val x: Int)
<!VALUE_CLASS_NOT_FINAL!>sealed<!> <!INLINE_CLASS_DEPRECATED!>inline<!> class NotFinalClass3(val x: Int)

<!VALUE_CLASS_CANNOT_BE_CLONEABLE, VALUE_CLASS_WITHOUT_JVM_INLINE_ANNOTATION!>value<!> class CloneableClass1(val x: Int): Cloneable
<!INLINE_CLASS_DEPRECATED, VALUE_CLASS_CANNOT_BE_CLONEABLE!>inline<!> class CloneableClass2(val x: Int): <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Cloneable<!>

open class Test
<!INLINE_CLASS_DEPRECATED!>inline<!> class ExtendTest(val x: Int): <!SUPERTYPE_NOT_INITIALIZED, VALUE_CLASS_CANNOT_EXTEND_CLASSES!>Test<!>

<!INLINE_CLASS_DEPRECATED!>inline<!> class ImplementByDelegation(val x: Int) : Comparable<Int> by x

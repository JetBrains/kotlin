class A {
    <!INLINE_CLASS_NOT_TOP_LEVEL!>inline<!> inner class B(val x: Int)
    fun foo() {
        <!INLINE_CLASS_NOT_TOP_LEVEL, WRONG_MODIFIER_TARGET!>inline<!> class C(val x: Int)
    }
    inner <!INLINE_CLASS_NOT_TOP_LEVEL!>value<!> class D(val x: Int)
}

<!INLINE_CLASS_NOT_FINAL!>open<!> inline class NotFinalClass1(val x: Int)
<!INLINE_CLASS_NOT_FINAL!>abstract<!> inline class NotFinalClass2(val x: Int)
<!INLINE_CLASS_NOT_FINAL!>sealed<!> inline class NotFinalClass3(val x: Int)

<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>value<!> class CloneableClass1(val x: Int): Cloneable
<!VALUE_CLASS_CANNOT_BE_CLONEABLE!>inline<!> class CloneableClass2(val x: Int): java.lang.Cloneable

open class Test
inline class ExtendTest(val x: Int): <!INLINE_CLASS_CANNOT_EXTEND_CLASSES, SUPERTYPE_NOT_INITIALIZED!>Test<!>

inline class ImplementByDelegation(val x: Int) : <!INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION!>Comparable<Int><!> by x

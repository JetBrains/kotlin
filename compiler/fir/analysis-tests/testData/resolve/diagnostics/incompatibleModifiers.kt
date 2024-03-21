// LANGUAGE: +DataClassCopyRespectsConstructorVisibility
<!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>public<!> class B
<!WRONG_MODIFIER_CONTAINING_DECLARATION!>protected<!> <!INCOMPATIBLE_MODIFIERS!>internal<!> class C

<!INCOMPATIBLE_MODIFIERS!>final<!> <!INCOMPATIBLE_MODIFIERS!>abstract<!> class D
<!INCOMPATIBLE_MODIFIERS!>final<!> <!INCOMPATIBLE_MODIFIERS!>open<!> class E
<!INCOMPATIBLE_MODIFIERS!>final<!> <!INCOMPATIBLE_MODIFIERS!>sealed<!> class F
<!INCOMPATIBLE_MODIFIERS!>open<!> <!INCOMPATIBLE_MODIFIERS!>sealed<!> class G

inline fun foo(
    <!INCOMPATIBLE_MODIFIERS!>crossinline<!> <!INCOMPATIBLE_MODIFIERS!>noinline<!> first: () -> Unit,
    second: () -> Unit
) {  }

<!INCOMPATIBLE_MODIFIERS, REDUNDANT_MODIFIER!>open<!> <!INCOMPATIBLE_MODIFIERS!>abstract<!> <!INCOMPATIBLE_MODIFIERS!>data<!> class H(val i: Int)
<!INCOMPATIBLE_MODIFIERS!>sealed<!> <!INCOMPATIBLE_MODIFIERS!>data<!> class I(val i: Int)
<!INCOMPATIBLE_MODIFIERS!>inline<!> <!INCOMPATIBLE_MODIFIERS!>data<!> class J(val i: Int)

abstract class K {
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>open<!> val i1: Int = 0
    <!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>abstract<!> val i2: Int
}

private open <!INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER_ERROR!>class L<!> : K()
private abstract class M : K()

class X {
    <!INCOMPATIBLE_MODIFIERS!>inner<!> <!INCOMPATIBLE_MODIFIERS!>data<!> class Y(val i: Int)
    <!INCOMPATIBLE_MODIFIERS!>sealed<!> <!INCOMPATIBLE_MODIFIERS!>inner<!> class Z
}

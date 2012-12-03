package illegal_modifiers

abstract class A() {
    <!INCOMPATIBLE_MODIFIERS!>abstract<!> <!INCOMPATIBLE_MODIFIERS!>final<!> fun f()
    abstract <!REDUNDANT_MODIFIER!>open<!> fun g()
    <!INCOMPATIBLE_MODIFIERS!>final<!> <!INCOMPATIBLE_MODIFIERS!>open<!> fun h() {}

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>open var r: String<!>
    get
    <!ILLEGAL_MODIFIER!>abstract<!> protected set
}

<!TRAIT_CAN_NOT_BE_FINAL!>final<!> trait T {}

class FinalClass() {
    <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> fun foo() {}
    val i: Int = 1
        <!ILLEGAL_MODIFIER!>open<!> get(): Int = $i
    var j: Int = 1
        <!ILLEGAL_MODIFIER!>open<!> set(v: Int) {}
}

<!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>public<!> class C
<!INCOMPATIBLE_MODIFIERS!>private<!> <!INCOMPATIBLE_MODIFIERS!>public<!> object D

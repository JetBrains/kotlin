<!DEPRECATED_MODIFIER_PAIR!>sealed<!> <!DEPRECATED_MODIFIER_PAIR!>data<!> class My(val x: Int) {
    object Your: My(1)
    class His(y: Int): My(y)
}

fun f() {
    try {} catch (<!ELEMENT!>: Any) {}

    try {} catch (@a <!ELEMENT!>: Any) {}
}

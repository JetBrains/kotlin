internal interface InternalInterface {
    val x: Any
}

class PublicClass : InternalInterface {
    <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>internal<!> val <!VIRTUAL_MEMBER_HIDDEN!>x<!>: Any = 42
}

internal interface InternalInterface {
    val x: Any
}

class PublicClass : InternalInterface {
    internal val <!VIRTUAL_MEMBER_HIDDEN!>x<!>: Any = 42
}

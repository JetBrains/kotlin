// FIR_IDENTICAL
external interface I {
    <!NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE!>fun foo(): Unit<!> = definedExternally

    val a: Int?
        get() = definedExternally

    var b: String?
        get() = definedExternally
        set(value) = definedExternally

    <!NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE!>val c: Int<!>
        get() = definedExternally

    <!NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE!>var d: String<!>
        get() = definedExternally
        set(value) = definedExternally
}
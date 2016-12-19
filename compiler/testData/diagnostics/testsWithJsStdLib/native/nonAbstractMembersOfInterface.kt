external interface I {
    <!NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE!>fun foo(): Unit<!> = noImpl

    val a: Int?
        get() = noImpl

    var b: String?
        get() = noImpl
        set(value) = noImpl

    <!NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE!>val c: Int<!>
        get() = noImpl

    <!NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE!>var d: String<!>
        get() = noImpl
        set(value) = noImpl

    var e: dynamic
        get() = noImpl
        set(value) = noImpl
}
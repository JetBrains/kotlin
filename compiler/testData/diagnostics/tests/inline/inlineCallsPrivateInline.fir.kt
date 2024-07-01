// RENDER_DIAGNOSTICS_FULL_TEXT
class AAA {
    inline fun <reified T> myFunction() {
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>localDeclarations<!>()
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privatePropInline<!>
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateVarPropInline<!> = ""
    }

    private <!NOTHING_TO_INLINE!>inline<!> fun localDeclarations(): Boolean {
        return true
    }

    private val privatePropInline: Int
        inline get() = 1

    private var privateVarPropInline: String
        get() = ""
        inline set(value) {}
}

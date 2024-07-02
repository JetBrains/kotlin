// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: JSubHolder.java
public class JSubHolder extends FunHolder {
}

// FILE: test.kt
open class FunHolder {
    private <!NOTHING_TO_INLINE!>inline<!> fun privateInlineFun(): Boolean {
        return true
    }

    internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineFun(): Boolean {
        return true
    }

    @PublishedApi
    internal <!NOTHING_TO_INLINE!>inline<!> fun publishedInternalInlineFun(): Boolean {
        return true
    }

    protected inline fun <reified T> protectedInlineCaller(
        privateInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateInlineFun<!>,
    internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
    ) {
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateInlineFun<!>()
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
        publishedInternalInlineFun()
    }

    inline fun <reified T> inlineCaller(
        privateInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateInlineFun<!>,
    internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
    ) {
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateInlineFun<!>()
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
    }

    protected val a : Any
        inline get() {
            <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateInlineFun<!>()
            <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
            publishedInternalInlineFun()
            return 1
        }

    val b : Any
        inline get() {
            <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateInlineFun<!>()
            <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
            return 1
        }

    inner class Inner {

        protected inline fun <reified T> protectedInlineCaller(
            privateInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateInlineFun<!>,
        internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
        ) {
            <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateInlineFun<!>()
            <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
            publishedInternalInlineFun()
        }

        inline fun <reified T> inlineCaller(
            privateInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateInlineFun<!>,
        internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
        ) {
            <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateInlineFun<!>()
            <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
            publishedInternalInlineFun()
        }
    }
}

inline fun <reified T> FunHolder.inlineExtensionCaller(
    internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
) {
    <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
    publishedInternalInlineFun()
}

class KSubHolder:FunHolder() {
    protected inline fun <reified T> protectedInlineSubCaller(
        internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
    ) {
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
        publishedInternalInlineFun()
    }

    inline fun <reified T> inlineSubCaller(
        internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
    ) {
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
        publishedInternalInlineFun()
    }

    inner class Inner {
        protected inline fun <reified T> protectedInlineSubCaller(
            internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
        ) {
            <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
            publishedInternalInlineFun()
        }

        inline fun <reified T> inlineSubCaller(
            internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
        ) {
            <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
            publishedInternalInlineFun()
        }
    }
}

inline fun <reified T> KSubHolder.inlineExtensionCaller(
    internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
) {
    <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
    publishedInternalInlineFun()
}

class KJSubHolder: JSubHolder() {
    protected inline fun <reified T> protectedInlineSubCaller(
        internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
    ) {
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
        publishedInternalInlineFun()
    }

    inline fun <reified T> inlineSubCaller(
        internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
    ) {
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
        publishedInternalInlineFun()
    }

    inner class Inner {
        protected inline fun <reified T> protectedInlineSubCaller(
            internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
        ) {
            <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
            publishedInternalInlineFun()
        }

        inline fun <reified T> inlineSubCaller(
            internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
        ) {
            <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
            publishedInternalInlineFun()
        }
    }
}

inline fun <reified T> KJSubHolder.inlineExtensionCaller(
    internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
) {
    <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
    publishedInternalInlineFun()
}

object FunHolderObject {
    private <!NOTHING_TO_INLINE!>inline<!> fun privateInlineFun(): Boolean {
        return true
    }

    internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineFun(): Boolean {
        return true
    }

    @PublishedApi
    internal <!NOTHING_TO_INLINE!>inline<!> fun publishedInternalInlineFun(): Boolean {
        return true
    }

    inline fun <reified T> inlineCaller(
        privateInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateInlineFun<!>,
    internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
    ) {
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateInlineFun<!>()
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
        publishedInternalInlineFun()
    }
}

inline fun <reified T> FunHolderObject.inlineExtensionCaller(
    internalInlineParam: () -> Boolean = ::<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>
) {
    <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalInlineFun<!>()
    publishedInternalInlineFun()
}

internal open class InternalHolder {
    private <!NOTHING_TO_INLINE!>inline<!> fun privateInlineDeclaration(): Boolean {
        return true
    }
    protected <!NOTHING_TO_INLINE!>inline<!> fun protectedInlineDeclaration(): Boolean {
        return true
    }
    internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineDeclaration(): Boolean {
        return true
    }
    <!NOTHING_TO_INLINE!>inline<!> fun publicInlineDeclarationPrivate() {
        privateInlineDeclaration()
    }
    <!NOTHING_TO_INLINE!>inline<!> fun publicInlineDeclarationInternal() {
        internalInlineDeclaration()
    }
    <!NOTHING_TO_INLINE!>inline<!> fun publicInlineDeclarationProtected() {
        protectedInlineDeclaration()
    }
}

inline fun <reified T> privateInlineFunc1(){
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>InternalHolder<!>().<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>publicInlineDeclarationPrivate<!>()
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>InternalHolder<!>().<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>publicInlineDeclarationInternal<!>()
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>InternalHolder<!>().<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>publicInlineDeclarationProtected<!>()
}

private open class PrivateHolder {
    private <!NOTHING_TO_INLINE!>inline<!> fun privateInlineDeclaration(): Boolean {
        return true
    }
    protected <!NOTHING_TO_INLINE!>inline<!> fun protectedInlineDeclaration(): Boolean {
        return true
    }
    internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineDeclaration(): Boolean {
        return true
    }
    <!NOTHING_TO_INLINE!>inline<!> fun publicInlineDeclarationPrivate() {
        privateInlineDeclaration()
    }
    <!NOTHING_TO_INLINE!>inline<!> fun publicInlineDeclarationInternal() {
        internalInlineDeclaration()
    }
    <!NOTHING_TO_INLINE!>inline<!> fun publicInlineDeclarationProtected() {
        protectedInlineDeclaration()
    }
}

inline fun <reified T> privateInlineFunc2(){
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>PrivateHolder<!>().<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>publicInlineDeclarationPrivate<!>()
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>PrivateHolder<!>().<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>publicInlineDeclarationInternal<!>()
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>PrivateHolder<!>().<!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>publicInlineDeclarationProtected<!>()
}

class PropHolder {
    private val privatePropInline: Int
        inline get() = 1

    internal val internalPropInline: Int
        inline get() = 2

    private var privateVarPropInline: String
        get() = ""
        inline set(value) {}

    internal var internalVarPropInline: String
        get() = ""
        inline set(value) {}

    <!NOTHING_TO_INLINE!>inline<!> fun myPublicFunction() {
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privatePropInline<!>
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalPropInline<!>
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>privateVarPropInline<!> = ""
        <!NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE!>internalVarPropInline<!> = ""
    }
}

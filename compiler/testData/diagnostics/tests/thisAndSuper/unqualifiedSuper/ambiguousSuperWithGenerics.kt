// !WITH_NEW_INFERENCE
open class GenericBaseClass<T> {
    open fun foo(x: T): T = x
    open fun ambiguous(x: T): T = x
}

interface GenericBaseInterface<T> {
    fun bar(x: T): T = x
    fun ambiguous(x: T): T = x
}

class GenericDerivedClass<T> : GenericBaseClass<T>(), GenericBaseInterface<T> {
    override fun foo(x: T): T = super.foo(x)
    override fun bar(x: T): T = super.bar(x)

    override fun ambiguous(x: T): T =
            <!NI;TYPE_MISMATCH!><!AMBIGUOUS_SUPER!>super<!>.<!NI;TYPE_MISMATCH!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>ambiguous<!>(x)<!><!>
}

class <!CONFLICTING_JVM_DECLARATIONS!>SpecializedDerivedClass<!> : GenericBaseClass<Int>(), GenericBaseInterface<String> {
    override fun foo(x: Int): Int = super.foo(x)
    override fun bar(x: String): String = super.bar(x)

    override fun ambiguous(x: String): String =
            <!NI;TYPE_MISMATCH!><!AMBIGUOUS_SUPER!>super<!>.<!NI;TYPE_MISMATCH!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>ambiguous<!>(x)<!><!>
    override fun ambiguous(x: Int): Int =
            <!NI;TYPE_MISMATCH!><!AMBIGUOUS_SUPER!>super<!>.<!NI;TYPE_MISMATCH!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>ambiguous<!>(x)<!><!>
}

class <!CONFLICTING_JVM_DECLARATIONS!>MixedDerivedClass<!><T> : GenericBaseClass<Int>(), GenericBaseInterface<T> {
    override fun foo(x: Int): Int = super.foo(x)
    override fun bar(x: T): T = super.bar(x)

    override fun ambiguous(x: Int): Int =
            <!NI;TYPE_MISMATCH!><!AMBIGUOUS_SUPER!>super<!>.<!NI;TYPE_MISMATCH!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>ambiguous<!>(x)<!><!>
    <!CONFLICTING_JVM_DECLARATIONS!>override fun ambiguous(x: T): T<!> =
            <!NI;TYPE_MISMATCH!><!AMBIGUOUS_SUPER!>super<!>.<!NI;TYPE_MISMATCH!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>ambiguous<!>(x)<!><!>
}
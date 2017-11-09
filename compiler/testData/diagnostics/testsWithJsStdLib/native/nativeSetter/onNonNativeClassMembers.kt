// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

class A {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeSetter
    fun set(a: String, v: Any?): Any?<!> = null

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeSetter
    fun put(a: Number, v: String)<!> {}

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeSetter
    fun foo(a: Int, v: String)<!> {}

    companion object {
        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeSetter
        fun set(a: String, v: Any?): Any?<!> = null

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeSetter
        fun put(a: Number, v: String)<!> {}

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeSetter
        fun foo(a: Int, v: String)<!> {}
    }
}

class B {
    <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
    val foo = 0

    <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
    object Obj1 {}

    companion object {
        <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
        val foo = 0

        <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
        object Obj2 {}
    }
}

class C {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeSetter
    fun Int.set(a: String, v: Int)<!> {}

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeSetter
    fun Int.<!NATIVE_SETTER_WRONG_RETURN_TYPE!>set2<!>(a: Number, v: String?)<!> = "OK"

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeSetter
    fun Int.<!NATIVE_SETTER_WRONG_RETURN_TYPE!>set3<!>(a: Double, v: String?)<!> = "OK"

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN, NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
    fun set(): Any?<!> = null

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN, NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
    fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any?<!> = null

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN, NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
    fun set(a: String, v: Any, v2: Any)<!> {}

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeSetter
    fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>, v: Any?)<!> {}

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>@nativeSetter
    fun foo(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: String = "0.0"<!>, v: String)<!> = "OK"
}
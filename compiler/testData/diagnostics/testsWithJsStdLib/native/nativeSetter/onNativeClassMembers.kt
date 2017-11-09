// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external class A {
    @nativeSetter
    fun set(a: String, v: Any?): Any? = definedExternally

    @nativeSetter
    fun put(a: Number, v: String) { definedExternally }

    @nativeSetter
    fun foo(a: Int, v: String) { definedExternally }

    @nativeSetter
    fun set4(a: Double, v: String): Any = definedExternally

    @nativeSetter
    fun set5(a: Double, v: String): CharSequence = definedExternally

    companion object {
        @nativeSetter
        fun set(a: String, v: Any?): Any? = definedExternally

        @nativeSetter
        fun put(a: Number, v: String) { definedExternally }

        @nativeSetter
        fun foo(a: Int, v: String) { definedExternally }

        @nativeSetter
        fun set4(a: Double, v: String): Any = definedExternally

        @nativeSetter
        fun set5(a: Double, v: String): CharSequence = definedExternally
    }
}

external class B {
    <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
    val foo: Int = definedExternally

    <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
    object Obj1 {}

    companion object {
        <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
        val foo: Int = definedExternally

        <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
        object Obj2 {}
    }
}

external class C {
    @nativeSetter
    fun set6(a: Double, v: String): <!NATIVE_SETTER_WRONG_RETURN_TYPE!>Number<!> = definedExternally

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
    fun set(): Any?<!> = definedExternally

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
    fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any?<!> = definedExternally

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
    fun set(a: String, v: Any, v2: Any)<!> { definedExternally }

    @nativeSetter
    fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>, v: Any?) { definedExternally }

    @nativeSetter
    fun foo(a: Number, <!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>v: String = definedExternally<!>): String = definedExternally
}
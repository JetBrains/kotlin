// DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_PARAMETER

interface IBase {
    fun foo()
    fun bar()
    fun <T> qux(x: T)
}

class CDerived : IBase {
    override inline final <!OVERRIDE_BY_INLINE!>fun foo()<!> {}
    override inline <!OVERRIDE_BY_INLINE!>fun bar()<!> {}
    override inline final <!OVERRIDE_BY_INLINE!>fun <<!REIFIED_TYPE_PARAMETER_IN_OVERRIDE!>reified<!> T> qux(x: T)<!> {}

    class CNested : IBase {
        override inline final <!OVERRIDE_BY_INLINE!>fun foo()<!> {}
        override inline <!OVERRIDE_BY_INLINE!>fun bar()<!> {}
        override inline final <!OVERRIDE_BY_INLINE!>fun <<!REIFIED_TYPE_PARAMETER_IN_OVERRIDE!>reified<!> T> qux(x: T)<!> {}
    }

    val anObject = object : IBase {
        override inline final <!OVERRIDE_BY_INLINE!>fun foo()<!> {}
        override inline <!OVERRIDE_BY_INLINE!>fun bar()<!> {}
        override inline final <!OVERRIDE_BY_INLINE!>fun <<!REIFIED_TYPE_PARAMETER_IN_OVERRIDE!>reified<!> T> qux(x: T)<!> {}
    }

    fun aMethod() {
        class CLocal : IBase {
            override inline final <!OVERRIDE_BY_INLINE!>fun foo()<!> {}
            override inline <!OVERRIDE_BY_INLINE!>fun bar()<!> {}
            override inline final <!OVERRIDE_BY_INLINE!>fun <<!REIFIED_TYPE_PARAMETER_IN_OVERRIDE!>reified<!> T> qux(x: T)<!> {}
        }
    }
}

open class COpen : IBase {
    override inline final <!OVERRIDE_BY_INLINE!>fun foo()<!> {}
    override <!DECLARATION_CANT_BE_INLINED!>inline<!> fun bar() {}
    override inline final <!OVERRIDE_BY_INLINE!>fun <<!REIFIED_TYPE_PARAMETER_IN_OVERRIDE!>reified<!> T> qux(x: T)<!> {}

    open class COpenNested : IBase {
        override inline final <!OVERRIDE_BY_INLINE!>fun foo()<!> {}
        override <!DECLARATION_CANT_BE_INLINED!>inline<!> fun bar() {}
        override inline final <!OVERRIDE_BY_INLINE!>fun <<!REIFIED_TYPE_PARAMETER_IN_OVERRIDE!>reified<!> T> qux(x: T)<!> {}
    }

    val anObject = object : IBase {
        override inline final <!OVERRIDE_BY_INLINE!>fun foo()<!> {}
        override inline <!OVERRIDE_BY_INLINE!>fun bar()<!> {}
        override inline final <!OVERRIDE_BY_INLINE!>fun <<!REIFIED_TYPE_PARAMETER_IN_OVERRIDE!>reified<!> T> qux(x: T)<!> {}
    }

    fun aMethod() {
        open class COpenLocal : IBase {
            override inline final <!OVERRIDE_BY_INLINE!>fun foo()<!> {}
            override <!DECLARATION_CANT_BE_INLINED!>inline<!> fun bar() {}
            override inline final <!OVERRIDE_BY_INLINE!>fun <<!REIFIED_TYPE_PARAMETER_IN_OVERRIDE!>reified<!> T> qux(x: T)<!> {}
        }
    }
}


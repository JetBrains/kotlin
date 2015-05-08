enum class E {
    <!ILLEGAL_MODIFIER!>public<!> <!ILLEGAL_MODIFIER!>final<!> SUBCLASS {
        fun foo() {}
    },

    <!ILLEGAL_MODIFIER!>public<!> PUBLIC,
    <!ILLEGAL_MODIFIER!>protected<!> PROTECTED,
    <!ILLEGAL_MODIFIER!>private<!> PRIVATE,
    <!ILLEGAL_MODIFIER!>internal<!> INTERNAL,

    <!ILLEGAL_MODIFIER!>abstract<!> ABSTRACT,
    <!ILLEGAL_MODIFIER!>open<!> OPEN,
    <!ILLEGAL_MODIFIER!>override<!> OVERRIDE,
    <!ILLEGAL_MODIFIER!>final<!> FINAL,

    <!ILLEGAL_MODIFIER!>inner<!> INNER,
    <!ILLEGAL_MODIFIER!>annotation<!> ANNOTATION,
    <!ILLEGAL_MODIFIER!>enum<!> ENUM,
    <!ILLEGAL_MODIFIER!>out<!> OUT,
    <!ILLEGAL_MODIFIER!>in<!> IN,
    <!ILLEGAL_MODIFIER!>vararg<!> VARARG,
    <!ILLEGAL_MODIFIER!>reified<!> REIFIED
}

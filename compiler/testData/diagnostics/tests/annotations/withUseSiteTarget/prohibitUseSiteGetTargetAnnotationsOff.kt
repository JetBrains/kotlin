// FIR_IDENTICAL
// !LANGUAGE: -ProhibitUseSiteGetTargetAnnotations
annotation class Ann

<!REPEATED_ANNOTATION!>@get:Ann<!>
<!REPEATED_ANNOTATION!>@set:Ann<!>
@Ann
var mutableProperty: Int = 42
    <!INAPPLICABLE_TARGET_ON_PROPERTY_WARNING!>@get:Ann<!> get
    <!INAPPLICABLE_TARGET_ON_PROPERTY!>@set:Ann<!> set

<!REPEATED_ANNOTATION!>@get:Ann<!>
<!REPEATED_ANNOTATION!>@set:Ann<!>
@Ann
var mutableProperty_AnnWithoutTarget: Int = 42
    @Ann get
    @Ann set

<!REPEATED_ANNOTATION!>@get:Ann<!>
@Ann
val immutableProperty: Int = 42
    <!INAPPLICABLE_TARGET_ON_PROPERTY_WARNING!>@get:Ann<!> get

<!REPEATED_ANNOTATION!>@get:Ann<!>
@Ann
val immutableProperty_AnnWithoutTarget: Int = 42
    @Ann get

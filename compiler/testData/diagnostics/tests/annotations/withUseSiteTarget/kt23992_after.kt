// !LANGUAGE: +ProhibitUseSiteTargetAnnotationsOnSuperTypes

interface Foo

annotation class Ann

class E : <!ANNOTATION_ON_SUPERCLASS!>@field:Ann<!> <!ANNOTATION_ON_SUPERCLASS!>@get:Ann<!> <!ANNOTATION_ON_SUPERCLASS!>@set:Ann<!> <!ANNOTATION_ON_SUPERCLASS!>@setparam:Ann<!> Foo

interface G : @Ann Foo
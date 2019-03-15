// !LANGUAGE: -ProhibitUseSiteTargetAnnotationsOnSuperTypes

interface Foo

annotation class Ann

class E : <!ANNOTATION_ON_SUPERCLASS_WARNING!>@field:Ann<!> <!ANNOTATION_ON_SUPERCLASS_WARNING!>@get:Ann<!> <!ANNOTATION_ON_SUPERCLASS_WARNING!>@set:Ann<!> <!ANNOTATION_ON_SUPERCLASS_WARNING!>@setparam:Ann<!> Foo

interface G : @Ann Foo
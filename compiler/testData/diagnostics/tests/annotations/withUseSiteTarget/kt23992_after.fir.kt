// !LANGUAGE: +ProhibitUseSiteTargetAnnotationsOnSuperTypes

interface Foo

annotation class Ann

class E : @field:Ann @get:Ann @set:Ann @setparam:Ann Foo

interface G : @Ann Foo
// LANGUAGE_VERSION: 1.4
// DISABLE-ERRORS

interface Foo

annotation class Ann

class E : @field:Ann @get:Ann <caret>@set:Ann @setparam:Ann Foo

interface G : @Ann Foo
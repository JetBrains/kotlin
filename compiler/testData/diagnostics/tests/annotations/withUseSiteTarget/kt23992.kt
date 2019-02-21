interface Foo

annotation class Ann

class E : @field:<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!> @get:<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!> @set:<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!> @setparam:<!DEBUG_INFO_MISSING_UNRESOLVED!>Ann<!> Foo
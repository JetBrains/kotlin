// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71002

interface Foo

class Bar0: Foo<!NULLABLE_SUPERTYPE!>?<!>
class Bar1(foo: Foo): Foo<!NULLABLE_SUPERTYPE!>?<!> by foo

typealias F = Foo?
class Bar2: F

typealias F1 = Foo
typealias F2 = F1?
class Bar3: F2

class Bar4(foo: Foo): F by foo

interface Foo1

class Bar5: Foo<!NULLABLE_SUPERTYPE!>?<!>, Foo1<!NULLABLE_SUPERTYPE!>?<!>
class Bar6: F2, Foo1<!NULLABLE_SUPERTYPE!>?<!>

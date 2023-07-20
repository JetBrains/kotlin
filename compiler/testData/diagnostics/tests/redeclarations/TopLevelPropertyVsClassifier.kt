// FILE: a.kt

val <!REDECLARATION!>Test1<!> = null
class <!REDECLARATION!>Test1<!>

val <!REDECLARATION!>Test2<!> = null
interface <!REDECLARATION!>Test2<!>

val <!REDECLARATION!>Test3<!> = null
object <!REDECLARATION!>Test3<!>

val <!PACKAGE_OR_CLASSIFIER_REDECLARATION, REDECLARATION!>Test4<!> = null
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION, REDECLARATION!>Test4<!>
interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Test4<!>
object <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Test4<!>

val <!REDECLARATION!>Test5<!> = null
class <!REDECLARATION!>Test6<!>

// FILE: b.kt

class <!REDECLARATION!>Test5<!>
val <!REDECLARATION!>Test6<!> = null

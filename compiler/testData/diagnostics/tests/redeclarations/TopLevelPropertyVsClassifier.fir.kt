// RUN_PIPELINE_TILL: FRONTEND
// FILE: a.kt

val <!REDECLARATION!>Test1<!> = null
class <!REDECLARATION!>Test1<!>

val <!REDECLARATION!>Test2<!> = null
interface <!REDECLARATION!>Test2<!>

val <!REDECLARATION!>Test3<!> = null
object <!REDECLARATION!>Test3<!>

val <!REDECLARATION!>Test4<!> = null
class <!CLASSIFIER_REDECLARATION!>Test4<!>
interface <!CLASSIFIER_REDECLARATION!>Test4<!>
object <!CLASSIFIER_REDECLARATION!>Test4<!>

val <!REDECLARATION!>Test5<!> = null
class <!REDECLARATION!>Test6<!>

// FILE: b.kt

class <!REDECLARATION!>Test5<!>
val <!REDECLARATION!>Test6<!> = null

// FILE: f1.kt
package test

class A
class <!CLASSIFIER_REDECLARATION!>F1<!>

// FILE: A.kts
package test

val x = 1

class F1
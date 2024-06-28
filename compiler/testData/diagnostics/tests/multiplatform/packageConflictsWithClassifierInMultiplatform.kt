// MODULE: m1-common
// FILE: common.kt
package a

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>b<!>

// FILE: common2.kt
package c.<!PACKAGE_OR_CLASSIFIER_REDECLARATION{JVM}!>d<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
package a.<!PACKAGE_OR_CLASSIFIER_REDECLARATION!>b<!>

// FILE: jvm2.kt
package c

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>d<!>

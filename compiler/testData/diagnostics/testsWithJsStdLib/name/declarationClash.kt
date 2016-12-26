// FILE: first.kt
package foo

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!>
class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>C<!>

<!CONFLICTING_OVERLOADS!>fun f(): Int<!> = 23
<!CONFLICTING_OVERLOADS!>fun f(): Int<!> = 99

<!CONFLICTING_OVERLOADS!>fun g(): String<!> = "23"
<!CONFLICTING_OVERLOADS!>fun g(): Int<!> = 23

val <!REDECLARATION!>x<!>: Int = 42
val <!REDECLARATION!>x<!>: Int = 99

val <!REDECLARATION!>y<!>: String = "42"
val <!REDECLARATION!>y<!>: Int = 42

class B {
    <!CONFLICTING_OVERLOADS!>fun f(): Int<!> = 23
    <!CONFLICTING_OVERLOADS!>fun f(): Int<!> = 99

    <!CONFLICTING_OVERLOADS!>fun g(): String<!> = "23"
    <!CONFLICTING_OVERLOADS!>fun g(): Int<!> = 23

    val <!REDECLARATION!>x<!>: Int = 42
    val <!REDECLARATION!>x<!>: Int = 99

    val <!REDECLARATION!>y<!>: String = "42"
    val <!REDECLARATION!>y<!>: Int = 42
}

// FILE: second.kt
package foo

class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>

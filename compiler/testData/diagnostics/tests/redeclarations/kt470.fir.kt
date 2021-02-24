<!REDECLARATION!>val a : Int = 1<!>
<!REDECLARATION!>val a : Int = 1<!>
<!REDECLARATION!>val a : Int = 1<!>

<!REDECLARATION!>val b : Int = 1<!>
<!REDECLARATION!>val b : Int = 1<!>
<!REDECLARATION!>val b : Int = 1<!>
<!REDECLARATION!>val b : Int = 1<!>

<!CONFLICTING_OVERLOADS!>fun foo()<!> {} // and here too
<!CONFLICTING_OVERLOADS!>fun foo()<!> {} // and here
<!CONFLICTING_OVERLOADS!>fun foo()<!> {} // and here
<!CONFLICTING_OVERLOADS!>fun foo()<!> {} // and here

<!CONFLICTING_OVERLOADS!>fun bar()<!> {} // and here
<!CONFLICTING_OVERLOADS!>fun bar()<!> {} // and here
<!CONFLICTING_OVERLOADS!>fun bar()<!> {} // and here

class A {
    <!REDECLARATION!>val a : Int = 1<!>
    <!REDECLARATION!>val a : Int = 1<!>
    <!REDECLARATION!>val a : Int = 1<!>

    <!REDECLARATION!>val b : Int = 1<!>
    <!REDECLARATION!>val b : Int = 1<!>
    <!REDECLARATION!>val b : Int = 1<!>
    <!REDECLARATION!>val b : Int = 1<!>

    <!CONFLICTING_OVERLOADS!>fun foo()<!> {} // and here too
    <!CONFLICTING_OVERLOADS!>fun foo()<!> {} // and here
    <!CONFLICTING_OVERLOADS!>fun foo()<!> {} // and here
    <!CONFLICTING_OVERLOADS!>fun foo()<!> {} // and here

    <!CONFLICTING_OVERLOADS!>fun bar()<!> {} // and here
    <!CONFLICTING_OVERLOADS!>fun bar()<!> {} // and here
    <!CONFLICTING_OVERLOADS!>fun bar()<!> {} // and here
}

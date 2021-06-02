// FILE:a.kt
package a

val foo = 2
fun bar() {}

class B {
    val foo = 2
    fun bar() {}

    class C
}

// FILE:b.kt
package a.b.c

class D {
    class E {

    }
}


// FILE:c.kt
import<!SYNTAX!><!> ;
import <!SYNTAX!>.<!>
import a.<!SYNTAX!><!> ;
import <!SYNTAX!>.a.<!> ;
import <!SYNTAX!>.a.b<!> ;
import <!SYNTAX!>.a.B<!> ;
import a.B.<!SYNTAX!><!> ;
import a.B.C.<!SYNTAX!><!> ;
import a.B.foo.<!SYNTAX!><!> ;
import a.B.bar.<!SYNTAX!><!> ;
import a.b.<!SYNTAX!><!> ;
import a.b.c.<!SYNTAX!><!> ;
import a.<!SYNTAX!>%<!>.b.c.<!SYNTAX!><!> ;
import a.b.c.D.<!SYNTAX!><!> ;
import a.b.c.D.E.<!SYNTAX!><!> ;

// FILE:d.kt
import<!SYNTAX!><!>
import <!SYNTAX!>.<!>
import a.<!SYNTAX!><!>
import <!SYNTAX!>.a.<!>
import <!SYNTAX!>.a.b<!>
import <!SYNTAX!>.a.B<!>
import a.B.<!SYNTAX!><!> as<!SYNTAX!><!>
import a.B.<!SYNTAX!><!> as A
import a.B.<!SYNTAX!><!>
import a.B.C.<!SYNTAX!><!>
import a.B.foo.<!SYNTAX!><!>
import a.B.bar.<!SYNTAX!><!>
import a.b.<!SYNTAX!><!>
import a.b.c.<!SYNTAX!><!>
import a.<!SYNTAX!>%<!>.b.c.<!SYNTAX!><!>
import a.b.c.D.<!SYNTAX!><!>
import a.b.c.D.E.<!SYNTAX!><!>

import <!PACKAGE_CANNOT_BE_IMPORTED!>a<!><!SYNTAX!>?.<!><!SYNTAX!>b<!>

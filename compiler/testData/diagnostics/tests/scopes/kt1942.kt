//FILE:a/C.java
//KT-1942 Package local members from Java are visible in subclasses
package a;

public class C {
    int myValue;
}

//FILE:d.kt

package d

import a.C

class A : C() {
    fun test() {
        val <!UNUSED_VARIABLE!>v<!> = <!INVISIBLE_MEMBER!>myValue<!>
    }
}

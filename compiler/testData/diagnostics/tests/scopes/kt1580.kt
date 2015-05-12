//FILE:a.kt
//KT-1580 Can't access nested class/interface from other package
package lib
interface WithInner {
    interface Inner {
    }
}

//FILE:b.kt
package user

import lib.WithInner

fun main(<!UNUSED_PARAMETER!>a<!> : WithInner, <!UNUSED_PARAMETER!>b<!> : WithInner.Inner) {
}
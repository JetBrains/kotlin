// FILE: B.kt

package b

class A

typealias TA = A

// FILE: A.kt

package a

import b.TA

class MyClass : <!FINAL_SUPERTYPE!>TA<!>()

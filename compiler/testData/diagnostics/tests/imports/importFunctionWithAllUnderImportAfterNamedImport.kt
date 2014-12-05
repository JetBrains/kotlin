//FILE:mainFile.kt
//----------------------------------------------------------------------------------
package test

import testing.other.*
import testing.TestFun

// Resolve should be ambiguous
val a = <!OVERLOAD_RESOLUTION_AMBIGUITY!>TestFun<!>()


//FILE:testing.kt
//----------------------------------------------------------------------------------
package testing

class TestFun

//FILE:testingOther.kt
//----------------------------------------------------------------------------------
package testing.other

fun TestFun() = 12

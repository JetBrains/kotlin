//FILE:mainFile.kt
//----------------------------------------------------------------------------------
package test

import testing.other.*
import testing.TestFun

// Resolve shouldn't be ambiguous
val a = TestFun()


//FILE:testing.kt
//----------------------------------------------------------------------------------
package testing

class TestFun

//FILE:testingOther.kt
//----------------------------------------------------------------------------------
package testing.other

fun TestFun() = 12

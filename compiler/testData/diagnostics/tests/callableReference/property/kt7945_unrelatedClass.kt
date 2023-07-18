// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty1

class TestClass(var prop: Int)
open class OtherClass
fun OtherClass.test(prop: KProperty1<TestClass, Int>): Unit = throw Exception()
class OtherClass2: OtherClass() {
    val result = test(TestClass::<!UNRESOLVED_REFERENCE!>result<!>)
}

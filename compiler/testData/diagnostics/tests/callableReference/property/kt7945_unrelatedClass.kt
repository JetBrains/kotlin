// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KMemberProperty

class TestClass(var prop: Int)
open class OtherClass
fun OtherClass.test(prop: KMemberProperty<TestClass, Int>): Unit = throw Exception()
class OtherClass2: OtherClass() {
    val result = test(TestClass::<!UNRESOLVED_REFERENCE!>result<!>)
}

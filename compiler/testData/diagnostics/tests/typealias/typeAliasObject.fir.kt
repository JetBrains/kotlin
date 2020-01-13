object AnObject {
    val ok = "OK"
    fun foo() = "OK"
}

typealias TestObject = AnObject

val test11: AnObject = TestObject
val test12: TestObject = TestObject
val test13: String = TestObject.ok
val test14: String = TestObject.foo()

typealias TestObject2 = TestObject

val test21: AnObject = TestObject2
val test22: TestObject2 = TestObject2
val test23: String = TestObject2.ok
val test24: String = TestObject2.foo()

class ClassWithCompanion {
    companion object {
        val ok = "OK"
        fun foo() = "OK"
    }
}

typealias TestCWC = ClassWithCompanion

val test35: ClassWithCompanion.Companion = TestCWC
val test36 = TestCWC
val test37: String = TestCWC.<!UNRESOLVED_REFERENCE!>ok<!>
val test38: String = TestCWC.<!UNRESOLVED_REFERENCE!>foo<!>()

object AnObject {
    val ok = "OK"
    fun foo() = "OK"
}

typealias GenericTestObject<T> = AnObject

val test11: AnObject = GenericTestObject
val test12: GenericTestObject<*> = GenericTestObject
val test13: String = GenericTestObject.ok
val test14: String = GenericTestObject.foo()

class GenericClassWithCompanion<T> {
    companion object {
        val ok = "OK"
        fun foo() = "OK"
    }
}

typealias TestGCWC<T> = GenericClassWithCompanion<T>

val test25: GenericClassWithCompanion.Companion = TestGCWC
val test26 = TestGCWC
val test27: String = TestGCWC.<!UNRESOLVED_REFERENCE!>ok<!>
val test28: String = TestGCWC.<!UNRESOLVED_REFERENCE!>foo<!>()

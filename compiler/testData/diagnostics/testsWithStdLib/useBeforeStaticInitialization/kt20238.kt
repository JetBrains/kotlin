// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

<!POSSIBLE_INITIALIZATION_DEADLOCK!>enum class Enum(val y: String) {
    <!POTENTIALLY_UNINITIALIZED_PROPERTY!>ENTRY(<!POTENTIALLY_UNINITIALIZED_ACCESS!>EnumTest.x<!>) {
        override fun toString(): String = y
    };<!>
}<!>

<!POSSIBLE_INITIALIZATION_DEADLOCK!>interface EnumTest {
    companion object {
        val x = "OK"
        <!POTENTIALLY_UNINITIALIZED_PROPERTY!>val z = <!POTENTIALLY_UNINITIALIZED_ACCESS!>Enum.ENTRY.y<!><!>
    }
}<!>

//class Class {
//    init {
//        println("Class.<init>")
//    }
//    val y = ClassTest.y
//}
//
//interface ClassTest {
//    companion object {
//        init {
//            println("ClassTest.<clinit>")
//        }
//        val x = "OK"
//        val z = Class().y
//        val y = "yay"
//    }
//}

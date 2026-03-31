// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

enum class Enum(val y: String) {
    <!UNINITIALIZED_PROPERTY!>ENTRY(<!UNINITIALIZED_ACCESS!>EnumTest.x<!>) {
        override fun toString(): String = y
    };<!>
}

interface EnumTest {
    companion object {
        val x = "OK"
        <!UNINITIALIZED_PROPERTY!>val z = <!UNINITIALIZED_ACCESS!>Enum.ENTRY.y<!><!>
    }
}

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
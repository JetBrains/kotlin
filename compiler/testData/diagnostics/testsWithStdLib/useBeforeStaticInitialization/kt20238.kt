// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
<!POSSIBLE_INITIALIZATION_DEADLOCK!>enum class Enum(val y: String) {
    <!POSSIBLY_UNINITIALIZED_ENUM_ENTRY!>ENTRY(<!ACCESSING_DECLARATION_OF_POSSIBLY_INACCESSIBLE_CLASS!>EnumTest.x<!>) {
        override fun toString(): String = y
    };<!>
}<!>

<!POSSIBLE_INITIALIZATION_DEADLOCK!>interface EnumTest {
    companion object {
        val x = "OK"
        <!POSSIBLY_UNINITIALIZED_PROPERTY!>val z = <!ACCESSING_DECLARATION_OF_POSSIBLY_INACCESSIBLE_CLASS!>Enum.ENTRY.y<!><!>
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

/* GENERATED_FIR_TAGS: companionObject, enumDeclaration, enumEntry, interfaceDeclaration, objectDeclaration,
primaryConstructor, propertyDeclaration, stringLiteral */

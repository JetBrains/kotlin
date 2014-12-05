package a.b

class Some {
    class Nested {
        fun foo() {
            "".<caret>
        }

        fun String.memberExtFun() { }
    }

    val String.memberExtProp: Int get() = 1
}

fun String.extFun() { }
val String.extProp: Int get() = 1

// EXIST: { lookupString: "extFun", itemText: "extFun", tailText: "() for String in a.b", typeText: "Unit" }
// EXIST: { lookupString: "extProp", itemText: "extProp", tailText: " for String in a.b", typeText: "Int" }
// EXIST: { lookupString: "memberExtFun", itemText: "memberExtFun", tailText: "() for String in Some.Nested", typeText: "Unit" }
// EXIST: { lookupString: "memberExtProp", itemText: "memberExtProp", tailText: " for String in Some", typeText: "Int" }

package ppp

class C {
    fun xxx(p: Int) = ""

    fun foo() {
        xx<caret>
    }
}

fun C.xxx(p: Int) = 1
fun Any.xxx(c: Char) = 1
fun C.xxx(c: Char) = 1

// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "(p: Int)", typeText: "String" }
// EXIST: { lookupString: "xxx", itemText: "xxx", tailText: "(c: Char) for C in ppp", typeText: "Int" }
// NOTHING_ELSE

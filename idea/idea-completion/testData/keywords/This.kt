// FIR_COMPARISON
class Outer {
    class Nested {
        inner class Inner {
            fun String.foo() {
                takeHandler1 {
                    takeHandler2({
                                     takeHandler3 { this<caret> }
                                 })
                }
            }
        }
    }
}

fun takeHandler1(handler: Int.() -> Unit){}
fun takeHandler2(handler: Char.() -> Unit){}
fun takeHandler3(handler: Char.() -> Unit){}

// INVOCATION_COUNT: 1
// EXIST: { lookupString: "this", itemText: "this", tailText: null, typeText: "Char", attributes: "bold" }
// ABSENT: "this@takeHandler3"
// EXIST: { lookupString: "this@takeHandler2", itemText: "this", tailText: "@takeHandler2", typeText: "Char", attributes: "bold" }
// EXIST: { lookupString: "this@takeHandler1", itemText: "this", tailText: "@takeHandler1", typeText: "Int", attributes: "bold" }
// EXIST: { lookupString: "this@foo", itemText: "this", tailText: "@foo", typeText: "String", attributes: "bold" }
// EXIST: { lookupString: "this@Inner", itemText: "this", tailText: "@Inner", typeText: "Outer.Nested.Inner", attributes: "bold" }
// EXIST: { lookupString: "this@Nested", itemText: "this", tailText: "@Nested", typeText: "Outer.Nested", attributes: "bold" }
// ABSENT: "this@Outer"
// NOTHING_ELSE

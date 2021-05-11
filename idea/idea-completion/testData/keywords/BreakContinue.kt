// FIR_COMPARISON
fun foo() {
    myFor@
    for (i in 1..10) {
        myWhile@
        while (x()) {
            myDo@
            do {
                <caret>
            } while (y())
        }
    }
}

// EXIST: { lookupString: "break", itemText: "break", tailText: null, attributes: "bold" }
// EXIST: { lookupString: "continue", itemText: "continue", tailText: null, attributes: "bold" }
// EXIST: { lookupString: "break@myDo", itemText: "break", tailText: "@myDo", attributes: "bold" }
// EXIST: { lookupString: "continue@myDo", itemText: "continue", tailText: "@myDo", attributes: "bold" }
// EXIST: { lookupString: "break@myWhile", itemText: "break", tailText: "@myWhile", attributes: "bold" }
// EXIST: { lookupString: "continue@myWhile", itemText: "continue", tailText: "@myWhile", attributes: "bold" }
// EXIST: { lookupString: "break@myFor", itemText: "break", tailText: "@myFor", attributes: "bold" }
// EXIST: { lookupString: "continue@myFor", itemText: "continue", tailText: "@myFor", attributes: "bold" }

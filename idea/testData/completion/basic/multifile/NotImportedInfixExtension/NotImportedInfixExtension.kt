package pack

class C

fun f() {
    C() <caret>
}

// INVOCATION_COUNT: 2
// ABSENT: "xxx"
// EXIST: { lookupString: "yyy", attributes: "bold" }
// ABSENT: "zzz"
// ABSENT: "extensionProp"

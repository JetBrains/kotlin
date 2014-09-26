package pack

class C

fun f() {
    C() <caret>
}

// INVOCATION_COUNT: 2
// ABSENT: "xxx"
// EXIST: "yyy"
// ABSENT: "zzz"
// ABSENT: "extensionProp"

// !LANGUAGE: +NewInference
// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

fun takeArray(array: Array<String>) {}

fun test() {
    "foo bar".<!UNRESOLVED_REFERENCE!>split<!>([""])
    <!UNRESOLVED_REFERENCE!>unresolved<!>([""])
    takeArray([""])
    val v = [""]
    [""]
    [1, 2, 3].<!UNRESOLVED_REFERENCE!>size<!>
}

fun baz(arg: Array<Int> = []) {
    if (true) ["yes"] else {["no"]}
}

class Foo(
    val v: Array<Int> = []
)

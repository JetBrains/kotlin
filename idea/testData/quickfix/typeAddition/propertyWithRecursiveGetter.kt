// "Specify type explicitly" "false"
// ERROR: Type checking has run into a recursive problem. Easiest workaround: specify types of your declarations explicitly
// ACTION: Convert member to extension
// ACTION: Convert property to function
// ACTION: Move to companion object

class A {
    val a<caret>
        get() = a
}

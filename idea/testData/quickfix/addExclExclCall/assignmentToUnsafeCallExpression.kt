// "Add non-null asserted (!!) call" "true"
class A(var s: String)

fun foo(a: A?) {
    a<caret>.s = ""
}

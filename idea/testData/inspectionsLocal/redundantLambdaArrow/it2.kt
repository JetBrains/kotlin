class A

fun A.foo(f: (String) -> Unit) {}

fun print(s: String) {}

fun bar() {
    A().foo { <caret>it ->
        print(it)
        print(it)
    }
}
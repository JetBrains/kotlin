// "Change getter type to (String) -> Int" "true"
class A {
    val x: (String) -> Int
        get(): Int<caret> = {42}
}
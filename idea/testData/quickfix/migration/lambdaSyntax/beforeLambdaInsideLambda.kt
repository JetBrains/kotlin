// "Migrate lambda syntax" "true"

class A

fun foo(a: Any) {}

val a = foo  a@ {

    val a = { <caret>(): Int -> 1 }
}
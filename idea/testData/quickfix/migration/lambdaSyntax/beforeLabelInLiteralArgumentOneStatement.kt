// "Migrate lambda syntax" "true"

class A

fun foo(a: Any) {}

val a = foo  a@ { <caret>Int.(a: String): A ->
     A()
}
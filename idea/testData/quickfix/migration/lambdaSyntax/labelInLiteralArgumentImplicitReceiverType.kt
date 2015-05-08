// "Migrate lambda syntax" "true"

class A

fun foo(a: Int.(String) -> A) {}

val a = foo  a@ { <caret>(a: String): A ->
     A()
     A()
}
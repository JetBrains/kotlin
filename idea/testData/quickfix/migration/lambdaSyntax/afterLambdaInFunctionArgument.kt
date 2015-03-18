// "Migrate lambda syntax" "true"

class A

fun foo(a: (Int).(String) -> Int) {

}


val a = foo (fun Int.(a: String): Int = 4)

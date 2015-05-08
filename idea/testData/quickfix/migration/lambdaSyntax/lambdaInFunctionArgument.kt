// "Migrate lambda syntax" "true"

class A

fun foo(a: (Int).(String) -> Int) {

}


val a = foo ({
    <caret>(Int).(a: String) -> 4
})

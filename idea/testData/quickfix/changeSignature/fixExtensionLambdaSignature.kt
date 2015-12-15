// "Change the signature of lambda expression" "true"

fun foo(f: Int.(Int, Int) -> Int) {

}

fun test() {
    foo { <caret>a: Int -> 0 }
}
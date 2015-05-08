// "Change the signature of function literal" "true"

fun foo(f: Int.(Int, Int) -> Int) {

}

fun test() {
    foo { <caret>(a: Int) -> 0 }
}
// "Change the signature of function literal" "true"

fun foo(f: Int.(Int, Int) -> Int) {

}

fun test() {
    foo {(i: Int, i1: Int) -> 0 }
}
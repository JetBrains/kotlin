// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

class A

fun foo(x: List<<selection>A?</selection>>) {

}

fun test() {
    foo(listOf(A()))
}
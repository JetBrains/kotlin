fun foo(action: () -> Unit) {

}

fun bar(i: Int) = "str#$i"

foo {
    <expr>bar(3)</expr>
}

val prop = bar(4)

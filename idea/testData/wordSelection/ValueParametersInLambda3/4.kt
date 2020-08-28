fun foo(f: (Int) -> Int) {}

fun test() {
    foo { it ->
<selection>        <caret>it + 1

        it + 1
</selection>    }
}
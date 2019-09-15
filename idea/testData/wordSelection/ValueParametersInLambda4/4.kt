fun foo(f: (Int) -> Int) {}

fun test() {
    foo { it ->
<selection>        it + 1

        <caret>it + 1
</selection>    }
}
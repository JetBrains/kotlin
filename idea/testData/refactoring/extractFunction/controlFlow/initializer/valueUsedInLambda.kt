// SIBLING:
fun main(args: Array<String>) {
    <selection>val a = 1</selection>
    lambda {
        a
    }
}

fun lambda(f: () -> Unit) {}
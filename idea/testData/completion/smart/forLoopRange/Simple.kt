fun foo(p1: String, p2: Int, p3: Collection<Int>) {
    for (i in <caret>)
}

fun f1(): String{}
fun f2(): Int{}
fun f3(): Iterable<Char>{}

// EXIST: p1
// ABSENT: p2
// EXIST: p3
// EXIST: f1
// ABSENT: f2
// EXIST: f3
// EXIST: { lookupString:"listOf", itemText: "listOf", tailText: "(vararg values: T) (kotlin)", typeText:"List<T>" }


interface X
interface Y: X
interface Z

fun foo(p1: Collection<X>, p2: Collection<Y>, p3: Collection<Z>, p4: Collection<X?>) {
    for (i: X in <caret>)
}

// EXIST: p1
// EXIST: p2
// ABSENT: p3
// ABSENT: p4
// EXIST: { lookupString:"listOf", itemText: "listOf", tailText: "(vararg elements: T) (kotlin)", typeText:"List<T>" }

fun foo() {
    for (@volatile x in z) {}

    for ([ann]) {}
    for (@ in z) {}

    for ((x, private data @ann [ann] y) in x) {}

    for (([ann], x) in pair) {}

    for (volatile x in 1..100) {}
    for (volatile(1) x in 1..100) {}
    for (volatile() (x, volatile y) in 1..100) {}
    for (volatile (x, volatile y) in 1..100) {}

    for (volatile var x in 1..100) {}
    for (volatile val (x, volatile y) in 1..100) {}

    for (private volatile var x in 1..100) {}
    for (private volatile val (x, volatile y) in 1..100) {}
}

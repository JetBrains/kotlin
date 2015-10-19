fun foo() {
    for (@Volatile x in z) {}

    for (@[ann]) {}
    for (@ in z) {}

    for ((x, private data @ann @[ann] y) in x) {}

    for ((@[ann], x) in pair) {}

    for (@Volatile x in 1..100) {}
    for (@Volatile(1) x in 1..100) {}
    for (@Volatile() (x, @Volatile y) in 1..100) {}

    for (@Volatile var x in 1..100) {}

    for (private @Volatile var x in 1..100) {}
    for (private @Volatile val (x, @Volatile y) in 1..100) {}
}

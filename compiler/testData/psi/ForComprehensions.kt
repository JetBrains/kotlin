fun main() {
    for (x in a) yield e
    for (x in a) yield {}
    for (x in a) yield {
        foo()
        e
    }
    for (x in a) yield for (y in b) yield e

    for (x in a) {
        foo()
        yield e
    }
    yield x
}
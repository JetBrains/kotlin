fun test() {
    Single.just(Object())
    .map {
    it
    }.map {
    it // The code unexpectedly shifts to the left
    }
}
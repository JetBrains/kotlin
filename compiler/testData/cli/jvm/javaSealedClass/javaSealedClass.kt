fun test(sj: SealedJava) = when (sj) {
    is SubSealedAJava -> "O"
    is SubSealedBJava -> "K"
}

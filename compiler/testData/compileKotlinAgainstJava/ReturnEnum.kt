package test

private fun getKind() = when(ReturnEnum().getKind()) {
    Kind.FIRST -> println(1)
    Kind.SECOND -> println(2)
    Kind.THIRD -> println(42)
}

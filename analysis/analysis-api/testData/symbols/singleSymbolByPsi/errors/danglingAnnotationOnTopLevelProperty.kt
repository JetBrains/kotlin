annotation class Ann

@Ann(value = {
    @Ann
    val <caret>localVal = 0
}
annotation class Ann

class C {
    @Ann(value = {
        @Ann
        val <caret>localVal = 0
    }
}
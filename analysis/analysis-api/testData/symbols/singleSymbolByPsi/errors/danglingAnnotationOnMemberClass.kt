annotation class Ann

class C {
    @Ann(value = {
        @Ann
        class <caret>LocalClass
    }
}
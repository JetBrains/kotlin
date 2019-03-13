import Owner.<caret>CompTest

class <caret>Owner {
    companion object CompTest {
        const val some = ""
    }
}

class User {
    val anything = CompTest.some
}
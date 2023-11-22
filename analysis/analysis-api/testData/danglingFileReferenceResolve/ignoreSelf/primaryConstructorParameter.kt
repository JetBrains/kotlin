// COPY_RESOLUTION_MODE: IGNORE_SELF

class Foo(name: String) {
    private val userName: String

    init {
        userName = n<caret>ame
    }
}
// "Add annotation target" "true"

annotation class Foo

class Test {
    fun foo(): <caret>@Foo Int = 1
}
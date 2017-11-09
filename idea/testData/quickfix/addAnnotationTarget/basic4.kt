// "Add annotation target" "true"

annotation class Foo

@Foo
class Test {
    @Foo
    fun foo(): <caret>@Foo Int = 1
}
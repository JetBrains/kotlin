// "Wrap with []" "true"

annotation class Foo(val value: Array<String>)

@Foo(value = "abc"<caret>)
class Bar
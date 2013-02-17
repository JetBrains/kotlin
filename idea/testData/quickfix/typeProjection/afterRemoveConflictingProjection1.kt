// "Remove 'in' modifier" "true"
class Foo<out T> {}

fun bar(foo : Foo<<caret>Any>) {}
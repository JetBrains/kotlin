// "Remove 'out' modifier" "true"
class Foo<in T> {}

fun bar(foo : Foo<out<caret> Any>) {}
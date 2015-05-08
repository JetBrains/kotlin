// "Remove redundant 'in' modifier" "true"
class Foo<in T> {
    val x = 0
}

fun bar(x : Foo< in<caret>  Any>) {

}

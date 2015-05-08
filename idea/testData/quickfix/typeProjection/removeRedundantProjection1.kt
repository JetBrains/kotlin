// "Remove redundant 'out' modifier" "true"
class Foo<out T> {
    val x = 0
}

fun bar(x : Foo< out<caret>  Any>) {

}

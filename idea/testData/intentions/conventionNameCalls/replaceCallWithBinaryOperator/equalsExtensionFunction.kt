// IS_APPLICABLE: false

class Foo

fun Foo?.equals(other: Foo?) = true

fun bar(f1: Foo?, f2: Foo?) {
    if (f1.equals<caret>(f2)) {

    }
}
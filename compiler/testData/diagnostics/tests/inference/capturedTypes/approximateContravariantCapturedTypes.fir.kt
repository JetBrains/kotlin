class Foo<T : Number>(var x: T) {
    fun setX1(y: T): T {
        this.x = y
        return y
    }
}

fun <T : Number> Foo<T>.setX(y: T): T {
    return y
}

class Foo2<T>(var x: T) {
    fun setX1(y: T): T {
        this.x = y
        return y
    }
}

fun <T> Foo2<T>.setX(y: T): T {
    this.x = y
    return y
}

fun Float.bar() {}

fun test1() {
    val fooSetRef = <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown"), UNRESOLVED_REFERENCE!>Foo<*>::setX<!>
    val foo = Foo<Float>(1f)

    fooSetRef.<!UNRESOLVED_REFERENCE!>invoke<!>(foo, 1)

    foo.x.bar()
}

fun test2() {
    val fooSetRef = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo<*>, CapturedType(*), CapturedType(*)>")!>Foo<*>::setX1<!>
    val foo = Foo<Float>(1f)

    fooSetRef.<!INAPPLICABLE_CANDIDATE!>invoke<!>(foo, 1)

    foo.x.bar()
}

fun test3() {
    val fooSetRef = <!DEBUG_INFO_EXPRESSION_TYPE("Type is unknown"), UNRESOLVED_REFERENCE!>Foo2<*>::setX<!>
    val foo = Foo2<Int>(1)

    fooSetRef.<!UNRESOLVED_REFERENCE!>invoke<!>(foo, "")

    foo.x.<!INAPPLICABLE_CANDIDATE!>bar<!>()
}

fun test4() {
    val fooSetRef = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction2<Foo2<*>, CapturedType(*), CapturedType(*)>")!>Foo2<*>::setX1<!>
    val foo = Foo2<Int>(1)

    fooSetRef.<!INAPPLICABLE_CANDIDATE!>invoke<!>(foo, "")

    foo.x.<!INAPPLICABLE_CANDIDATE!>bar<!>()
}

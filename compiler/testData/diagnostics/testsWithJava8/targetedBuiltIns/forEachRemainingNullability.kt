import java.util.function.Consumer

abstract class MyIt1<out T> : Iterator<T> {
    override fun forEachRemaining(x: Consumer<in T>) {}
}

abstract class MyIt2<out T> : Iterator<T> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun forEachRemaining(x: Consumer<in T?>) {}
}

abstract class MyIt3<out T> : Iterator<T> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun forEachRemaining(x: Consumer<in T>?) {}
}

abstract class MyIt4 : Iterator<String?> {
    override fun forEachRemaining(x: Consumer<in String?>) {}
}

abstract class MyIt5 : Iterator<String> {
    override fun forEachRemaining(x: Consumer<in String>) {}
}

abstract class MyIt6 : Iterator<String?> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun forEachRemaining(x: Consumer<in String>) {}
}

abstract class MyIt7 : Iterator<String> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun forEachRemaining(x: Consumer<in String?>) {}
}


fun foo(x: Iterator<String>, y: Iterator<String?>) {
    x.forEachRemaining(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    x.forEachRemaining { it -> it.length }
    x.forEachRemaining { it -> it<!UNNECESSARY_SAFE_CALL!>?.<!>length }
    y.forEachRemaining { it -> it<!UNSAFE_CALL!>.<!>length }
    y.forEachRemaining { it -> it?.length }
}

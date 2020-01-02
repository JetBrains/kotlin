// FULL_JDK

import java.util.function.Consumer

abstract class MyIt1<out T> : Iterator<T> {
    override fun forEachRemaining(x: Consumer<in T>) {}
}

abstract class MyIt2<out T> : Iterator<T> {
    override fun forEachRemaining(x: Consumer<in T?>) {}
}

abstract class MyIt3<out T> : Iterator<T> {
    override fun forEachRemaining(x: Consumer<in T>?) {}
}

abstract class MyIt4 : Iterator<String?> {
    override fun forEachRemaining(x: Consumer<in String?>) {}
}

abstract class MyIt5 : Iterator<String> {
    override fun forEachRemaining(x: Consumer<in String>) {}
}

abstract class MyIt6 : Iterator<String?> {
    override fun forEachRemaining(x: Consumer<in String>) {}
}

abstract class MyIt7 : Iterator<String> {
    override fun forEachRemaining(x: Consumer<in String?>) {}
}


fun foo(x: Iterator<String>, y: Iterator<String?>) {
    x.forEachRemaining(null)

    x.forEachRemaining { it -> it.length }
    x.forEachRemaining { it -> it?.length }
    y.forEachRemaining { it -> it.<!INAPPLICABLE_CANDIDATE!>length<!> }
    y.forEachRemaining { it -> it?.length }
}

package test

import java.util.ArrayList

public trait LoadIterableWithPropagation: java.lang.Object {
    public trait LoadIterable<T> : java.lang.Object {
        public fun getIterable(): MutableIterable<T>?
        public fun setIterable(p0: MutableIterable<T>?)

        public fun getReadOnlyIterable(): Iterable<T>?
        public fun setReadOnlyIterable(p0: Iterable<T>?)
    }

    public open class LoadIterableImpl<T> : LoadIterable<T> {
        public override fun getIterable(): MutableIterable<T>? = ArrayList<T>()
        public override fun setIterable(p0: MutableIterable<T>?): Unit {}

        public override fun getReadOnlyIterable(): Iterable<T>? = ArrayList<T>()
        public override fun setReadOnlyIterable(p0: Iterable<T>?): Unit {}
    }
}

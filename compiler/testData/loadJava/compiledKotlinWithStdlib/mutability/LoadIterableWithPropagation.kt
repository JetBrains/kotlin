// JVM_ANNOTATIONS
package test

import java.util.ArrayList
import kotlin.annotations.jvm.*

public interface LoadIterableWithPropagation {
    public interface LoadIterable<T> {
        @Mutable
        public fun getIterable(): MutableIterable<T>?
        public fun setIterable(@Mutable p0: MutableIterable<T>?)

        @ReadOnly
        public fun getReadOnlyIterable(): Iterable<T>?
        public fun setReadOnlyIterable(@ReadOnly p0: Iterable<T>?)
    }

    public open class LoadIterableImpl<T> : LoadIterable<T> {
        public override fun getIterable(): MutableIterable<T>? = ArrayList<T>()
        public override fun setIterable(p0: MutableIterable<T>?): Unit {}

        public override fun getReadOnlyIterable(): Iterable<T>? = ArrayList<T>()
        public override fun setReadOnlyIterable(p0: Iterable<T>?): Unit {}
    }
}

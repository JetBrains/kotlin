// WITH_STDLIB

// JVM_ABI_K1_K2_DIFF: KT-63828

open class Content() {
    override fun toString() = "OK"
}

interface ContentBox<T : Content> : List<T>

object Impl : ContentBox<Content>, AbstractList<Content>() {
    override fun get(index: Int) = Content()

    override val size: Int
        get() = throw UnsupportedOperationException()
}

class ContentBoxDelegate<T : Content>() : ContentBox<T> by (Impl as ContentBox<T>)

fun box() = ContentBoxDelegate<Content>()[0].toString()

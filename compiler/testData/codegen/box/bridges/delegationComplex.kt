// IGNORE_BACKEND_FIR: JVM_IR
open class Content() {
    override fun toString() = "OK"
}

interface Box<E> {
    fun get(): E
}

interface ContentBox<T : Content> : Box<T>

object Impl : ContentBox<Content> {
    override fun get(): Content = Content()
}

class ContentBoxDelegate<T : Content>() : ContentBox<T> by (Impl as ContentBox<T>)

fun box() = ContentBoxDelegate<Content>().get().toString()

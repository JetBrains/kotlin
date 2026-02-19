abstract class ItemCallback<T> {
    abstract fun areContentsTheSame(oldItem: T, newItem: T): Boolean
}

sealed class MessagePreview {
    abstract override fun equals(other: Any?): Boolean
}

interface InterfaceWithEquals {
    override fun equals(other: Any?): Boolean
}

private fun <T> interfaceAndClass(): ItemCallback<T> where T : InterfaceWithEquals, T : MessagePreview = object : ItemCallback<T>() {
    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem
}

private fun <T> classAndInterface(): ItemCallback<T> where T : MessagePreview, T : InterfaceWithEquals = object : ItemCallback<T>() {
    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem
}

private fun <T : MessagePreview> classOnly() = object : ItemCallback<T>() {
    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = oldItem == newItem
}

fun check(i: InterfaceWithEquals, m: MessagePreview) {
    i == m
    i != m

    m == i
    m != i

    i.equals(m)
    m.equals(i)
}
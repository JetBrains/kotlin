abstract class ItemCallback<T> {
    abstract fun areContentsTheSame(oldItem: T, newItem: T): Boolean
}

sealed class MessagePreview {
    abstract override fun equals(other: Any?): Boolean
}

private fun <T : MessagePreview> createDiffCallback() =
    object : ItemCallback<T>() {
        override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = <expr>oldItem == newItem</expr>
    }
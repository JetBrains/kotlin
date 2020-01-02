val instance = My()

class My {
    val equalsInstance = (this == instance)

    val isInstance = if (this === instance) "true" else "false"

    override fun equals(other: Any?) =
            other is My && isInstance.hashCode() == other.isInstance.hashCode()
}

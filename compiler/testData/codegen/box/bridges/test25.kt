// WITH_STDLIB

abstract class AbstractCollectionSerializer<Collection, Builder> {
    abstract fun builder(): Builder
    abstract fun Builder.toResult(): Collection
    abstract fun Collection.toBuilder(): Builder
    open fun deserialize(): Collection { return builder().toResult() }
}

abstract class PrimitiveArraySerializer<Array, Builder : PrimitiveArrayBuilder<Array>> : AbstractCollectionSerializer<Array, Builder>() {
    final override fun Builder.toResult(): Array = build()
    final override fun builder(): Builder = empty().toBuilder()
    abstract fun empty(): Array
}

object UByteArraySerializer : PrimitiveArraySerializer<UByteArray, UByteArrayBuilder>() {
    override fun UByteArray.toBuilder(): UByteArrayBuilder = UByteArrayBuilder(this)
    override fun empty(): UByteArray = UByteArray(0)
}

abstract class PrimitiveArrayBuilder<Array> {
    internal abstract fun build(): Array
}

class UByteArrayBuilder(val buffer: UByteArray) : PrimitiveArrayBuilder<UByteArray>() {
    override fun build() = buffer.copyOf(buffer.size)
}

fun box(): String {
    val array: UByteArray = UByteArraySerializer.deserialize()
    return "OK"
}

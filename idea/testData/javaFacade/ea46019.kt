class SomeProp() {
    fun get(t: Any, metadata: PropertyMetadataImpl) = 42
}

class Some<caret>() {
    val renderer  by SomeProp()
}
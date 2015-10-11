class SomeProp() {
    fun getValue(t: Any, metadata: PropertyMetadataImpl) = 42
}

class Some<caret>() {
    val renderer  by SomeProp()
}
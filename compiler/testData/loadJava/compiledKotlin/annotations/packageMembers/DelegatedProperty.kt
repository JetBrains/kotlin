package test

annotation class Anno

@Anno val x: Int by object {
    fun getValue(thiz: Any?, data: PropertyMetadata) = null!!
}

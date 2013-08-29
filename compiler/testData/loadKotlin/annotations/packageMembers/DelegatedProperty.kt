package test

annotation class Anno

Anno val x: Int by object {
    fun get(thiz: Any?, data: PropertyMetadata) = null!!
}

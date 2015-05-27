class MyProperty {
    fun get(thisRef: Any?, desc: PropertyMetadata) = ":)"
}

val Any.ext by MyProperty()

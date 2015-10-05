class MyProperty {
    fun getValue(thisRef: Any?, desc: PropertyMetadata) = ":)"
}

val Any.ext by MyProperty()

class X<T> {
    class Y<K>
    open class BASE
    /*PLACE*/class PLACE
}

/*ONAIR*/inner class ONAIR : BASE() {
    fun x() = Y<T>()
}

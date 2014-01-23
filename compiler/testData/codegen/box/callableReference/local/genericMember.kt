fun box(): String {
    class Id<T> {
        fun invoke(t: T) = t
    }

    val ref = Id<String>::invoke
    return Id<String>().ref("OK")
}

inline fun callable(): Int {
    val obj = object : SomeClass() {
        fun compute() = call(InheritedType())
    }
    return obj.compute()
}

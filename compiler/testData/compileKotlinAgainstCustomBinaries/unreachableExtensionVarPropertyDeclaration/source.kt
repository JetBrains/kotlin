// KT-44496

class C {
    val todo: String = TODO()

    var String.noSetterExtensionProperty: Int
        get() = 42
}

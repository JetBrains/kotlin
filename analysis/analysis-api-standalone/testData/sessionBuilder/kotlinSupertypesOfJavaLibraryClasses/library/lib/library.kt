package lib

interface LibraryInterface {
    fun interfaceFunction(): Int
}

fun LibraryInterface.extensionFunction(): Int = 0

open class LibraryClass {
    fun classFunction(): Int = 0

    val classProperty: String get() = ""
}

interface GenericLibraryInterface<T> {
    fun get(): T
}

class LibraryOuterClass {
    interface NestedInterface
}

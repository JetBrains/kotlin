open class Base {
    fun function(b: ByteArray): Long = b.size.toLong()
}

actual class Foo : Base()

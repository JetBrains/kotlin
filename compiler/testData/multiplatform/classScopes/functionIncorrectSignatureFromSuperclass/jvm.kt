open class Base {
    fun function(b: ByteArray): Long = b.size.toLong()
}

impl class Foo : Base()

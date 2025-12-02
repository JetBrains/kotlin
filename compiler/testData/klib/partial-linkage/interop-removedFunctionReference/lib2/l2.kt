@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Baz : l1.Bar() {
    override fun foo() = 42
}

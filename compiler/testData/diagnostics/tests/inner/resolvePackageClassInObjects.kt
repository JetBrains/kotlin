// FIR_IDENTICAL
open class PackageTest

class MoreTest() {
    companion object: PackageTest() {

    }

    object Some: PackageTest()
}
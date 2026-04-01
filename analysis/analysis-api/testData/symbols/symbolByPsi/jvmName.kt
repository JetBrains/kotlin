// WITH_STDLIB

class Foo {
    @get:JvmName("getMyI")
    @set:JvmName("setMyI")
    var i: Int

    var j: Int
        @JvmName("getMyJ")
        get() = 1
        @JvmName("setMyJ")
        set(value) {
        }
}
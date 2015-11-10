package companionObjectWithStatics

class Foo {
    companion object {
        @JvmStatic fun foo() {
        }

        val CONST = 111
    }
}
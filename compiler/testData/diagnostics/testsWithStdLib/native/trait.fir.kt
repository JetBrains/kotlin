import kotlin.jvm.*

interface Tr {
    external fun foo()
    external fun bar() {}

    companion object {
        external fun foo()
        external fun bar() {}
    }
}
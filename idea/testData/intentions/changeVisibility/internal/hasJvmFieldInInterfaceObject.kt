// WITH_RUNTIME
interface KotlinInterface {
    object O {
        @JvmField
        <caret>val bar = Any()
    }
}
// RUN_PIPELINE_TILL: BACKEND
class Some {
    class Nested

    fun foo(): Nested {
        return Nested()
    }
}

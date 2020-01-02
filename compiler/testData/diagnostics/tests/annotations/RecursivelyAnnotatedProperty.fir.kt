// Properties can be recursively annotated
annotation class ann(val x: Int)
class My {
    @ann(x) val x: Int = 1
}
// ERROR: Property must be initialized
class Owner {
    var <caret>p: Int
        get() = 1
        set
}

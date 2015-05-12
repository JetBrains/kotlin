// ERROR: Property must be initialized
class Owner {
    var <caret>p: Int
        get() { return 1 }
}

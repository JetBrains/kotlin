// "Change to val" "false"
// ACTION: Add initializer
// ACTION: Convert member to extension
// ACTION: Initialize with constructor parameter
// ACTION: Introduce backing property
// ACTION: Move to companion object
// ACTION: Specify type explicitly
// ERROR: Property must be initialized
class Test {
    var foo<caret>
        get() {
            return 1
        }
}

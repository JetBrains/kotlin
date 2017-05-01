// "Add 'lateinit' modifier" "false"
// ACTION: Add initializer
// ACTION: Make 'a' abstract
// ACTION: Move to constructor parameters
// ACTION: Move to constructor
// ERROR: Property must be initialized or be abstract

class A {
    private var a: String?<caret>
}
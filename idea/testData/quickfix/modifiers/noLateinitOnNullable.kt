// "Add 'lateinit' modifier" "false"
// ACTION: Add initializer
// ACTION: Make 'a' abstract
// ACTION: Move to constructor parameters
// ACTION: Move to constructor
// ACTION: Add getter
// ACTION: Add getter and setter
// ACTION: Add setter
// ERROR: Property must be initialized or be abstract

class A {
    private var a: String?<caret>
}
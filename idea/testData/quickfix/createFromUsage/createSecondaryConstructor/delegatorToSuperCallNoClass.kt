// "Create secondary constructor" "false"
// ERROR: This class does not have a constructor
// ACTION: Remove constructor call

interface T {

}

class A: T(<caret>1) {

}
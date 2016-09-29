// "Create secondary constructor" "false"
// ERROR: Too many arguments for public constructor Any() defined in kotlin.Any
// ACTION: Convert to primary constructor
// WITH_RUNTIME

interface T {

}

class A: T {
    constructor(): super(<caret>1) {

    }
}
// "Create secondary constructor" "true"
// ERROR: Too many arguments for public/*package*/ constructor J() defined in J

internal class B: J {
    constructor(): super(<caret>1) {

    }
}
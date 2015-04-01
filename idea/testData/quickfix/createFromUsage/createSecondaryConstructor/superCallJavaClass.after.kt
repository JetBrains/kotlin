// "Create secondary constructor" "true"
// ERROR: Too many arguments for public/*package*/ constructor J() defined in J

class B: J {
    constructor(): super(1) {

    }
}
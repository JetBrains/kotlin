open class A: Z {

}

// INFO: {"checked": "true"}
interface X

// INFO: {"checked": "true"}
interface Y

// INFO: {"checked": "true"}
interface Z

class <caret>B: A(), X, Y, Z
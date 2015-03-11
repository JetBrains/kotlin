val y = 1

class A() {
    init {
        <!INACCESSIBLE_BACKING_FIELD!>$y<!> = 1
    }
}

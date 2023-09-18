fun resolve<caret>Me() {
    receive(withGetterAndSetter)
    withGetterAndSetter = 123
}

fun receive(value: Int) {}

var withGetterAndSetter: Int = 42
    get() = field
    set(value) {
        field = value
    }

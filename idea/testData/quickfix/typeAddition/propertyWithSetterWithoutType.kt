// "Specify type explicitly" "true"

class My {

    var yy = 0

    var <caret>y
        get() = yy
        set(arg: Int) {
            yy = arg + 1
        }
}
val Any?.foo: Int
    get() {
        this?.let {
            <caret>return 1
        }
        return 2
    }

public inline fun <T> T.let(block: (T) -> Unit) {}

//HIGHLIGHTED: return 1
//HIGHLIGHTED: return 2
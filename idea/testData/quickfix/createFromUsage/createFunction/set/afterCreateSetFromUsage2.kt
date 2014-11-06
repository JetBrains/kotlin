// "Create function 'set' from usage" "true"

import java.util.ArrayList

class Foo<T> {
    fun <T> x (y: Any, w: java.util.ArrayList<T>) {
        y["", w] = w
    }
}

fun <T> Any.set(s: String, w: ArrayList<T>, value: ArrayList<T>) {
    throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
}

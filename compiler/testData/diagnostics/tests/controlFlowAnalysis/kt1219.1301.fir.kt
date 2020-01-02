//KT-1219 Incorrect 'unused value' error in closures

package kt1219

fun <T, R> Iterable<T>.fold(a: R, op: (T, R) -> R) : R {
    var r = a
    this.foreach { r = op(it, r) } //unused value here
    return r
}

//KT-1301 Modification of local of outer function in a local function should not be marked as unused assignment
fun foo(){
    var local = 0
    fun bar(){
        local = 1
    }

    bar()
    System.out.println(local)
}

fun <T> Iterable<T>.foreach(operation: (element: T) -> Unit) {
  for (elem in this)
    operation(elem)
}

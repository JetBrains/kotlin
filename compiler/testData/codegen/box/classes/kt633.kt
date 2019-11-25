// IGNORE_BACKEND_FIR: JVM_IR
class mInt(val i : Int) {
    override fun toString() : String = "mint: $i"
    operator fun plus(i : Int) = mInt(this.i + i)
    operator fun inc() = mInt(i + 1)
}

class MyArray() {
    val a = Array<mInt>(10, {mInt(0)})
    operator fun get(i : mInt) : mInt = a[i.i]
    operator fun set(i : mInt, v : mInt) {
        a[i.i] = v
    }
}

fun box() : String {
    val a = MyArray()
    var i = mInt(0)
    a[i++]
    a[i++] = mInt(1)
    for (i in 0..9)
        a[mInt(i)]
    return "OK"
}

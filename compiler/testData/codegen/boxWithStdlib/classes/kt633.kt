class mInt(val i : Int) {
    override fun toString() : String = "mint: $i"
    fun plus(i : Int) = mInt(this.i + i)
    fun inc() = mInt(i + 1)
}

class MyArray() {
    val a = Array<mInt>(10, {mInt(0)})
    fun get(i : mInt) : mInt = a[i.i]
    fun set(i : mInt, v : mInt) {
        a[i.i] = v
    }
}

fun box() : String {
    val a = MyArray()
    var i = mInt(0)
      System.out?.println(i)
    a[i++]// = mInt(1)
      System.out?.println(i)
    a[i++] = mInt(1)
      System.out?.println(i)
    for (i in 0..9)
      System.out?.println("ar: ${a[mInt(i)]}")
    return "OK"
}

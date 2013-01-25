import java.util.*

class A() {
    fun <T> ArrayList<T>.add3(el: T) = add(el)

    fun test(list: ArrayList<Int>) {
        for (i in 1..10) {
          list add3 i
        }
    }
}

fun <T> ArrayList<T>.add2(el: T) = add(el)

fun box() : String{
    var list = ArrayList<Int>()
    for (i in 1..10) {
      list add  i
      list add2 i
    }
    A().test(list)
    System.out?.println(list)
    return "OK"
}

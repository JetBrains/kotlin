
package kotlin1

import java.util.*

fun main(args : Array<String>) {
    val al : ArrayList<Int> = ArrayList<Int>()
    val al1 = ArrayList<Int>(1)
//    for (x in al1) {
//
//    }
    val <!UNUSED_VARIABLE!>al2<!> = ArrayList<Int>(ArrayList<Int>())
    al : RandomAccess
    al.clear() : Unit
    al.add(1) : Boolean
    al.add(0, 1) : Unit
    al.addAll(al1) : Boolean
    al.addAll(0, al1) : Boolean
    al.get(0) : Int
    val m = HashMap<String, Int>()
    m.put("", 1)
    test(al, m)
}
fun test(a : List<Int>, m : Map<String, Int>) {
  System.out.println(
    a.get(0) + 1
  )
  HashMap<Int, Int>().get(0)
  if (m.get("") != null)
  System.out.println(m.get("").sure().plus(1))
}

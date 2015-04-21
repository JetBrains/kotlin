// !CHECK_TYPE

package kotlin1

import java.util.*

fun main(args : Array<String>) {
    val al : ArrayList<Int> = ArrayList<Int>()
    val al1 = ArrayList<Int>(1)
//    for (x in al1) {
//
//    }
    val <!UNUSED_VARIABLE!>al2<!> = ArrayList<Int>(ArrayList<Int>())
    checkSubtype<RandomAccess>(al)
    checkSubtype<Unit>(al.clear())
    checkSubtype<Boolean>(al.add(1))
    checkSubtype<Unit>(al.add(0, 1))
    checkSubtype<Boolean>(al.addAll(al1))
    checkSubtype<Boolean>(al.addAll(0, al1))
    checkSubtype<Int>(al.get(0))
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
  System.out.println(m.get("")!!.plus(1))
}

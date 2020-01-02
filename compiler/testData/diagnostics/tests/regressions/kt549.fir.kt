//KT-549 type inference failed

package demo

  fun <T> filter(list : Array<T>, filter :  (T) -> Boolean) : List<T> {
    val answer = java.util.<!UNRESOLVED_REFERENCE!>ArrayList<!><T>();
    for (l in list) {
      if (filter(l)) answer.<!UNRESOLVED_REFERENCE!>add<!>(l)
    }
    return answer;
  }

fun main(args : Array<String>) {
  for (a in filter(args, {it.length > 1})) {
    System.out.println("Hello, ${a}!")
  }
}

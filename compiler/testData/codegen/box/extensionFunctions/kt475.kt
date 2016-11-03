// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

import java.util.ArrayList

fun box() : String {
  val array = ArrayList<String>()
  array.add("0")
  array.add("1")
  array.add("2")
  array.last = "5"
  return if(array.length == 3 && array.last == "5") "OK" else "fail"
}

var <T> ArrayList<T>.length : Int
    get() = size
    set(value: Int) = throw java.lang.Error()

var <T> ArrayList<T>.last : T
    get() = get(size-1)!!
    set(el : T) { set(size-1, el) }

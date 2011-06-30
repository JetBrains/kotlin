class Box<T>(t: T) {
    var value = t
}

fun box(): String {
  val nullableBox = Box<String?>("")
  val notnullBox = Box<String>("")
  if (nullableBox is Box<String>) return "fail 1"
  if (notnullBox is Box<String?>) return "fail 2"
  if (nullableBox !is Box<String?>) return "fail 3"
  if (notnullBox !is Box<String>) return "fail 4"
  return "OK"
}

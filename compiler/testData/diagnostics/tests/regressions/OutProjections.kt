class Point() {
}

class G<T>() {}

fun f<T>(<!UNUSED_PARAMETER!>expression<!> : T) : G<out T> = G<T>()


fun foo() : G<Point> {
  val p = Point()
  return <!TYPE_MISMATCH!>f<Point>(p)<!>
}

class Out<out T>() {}

fun fout<T>(<!UNUSED_PARAMETER!>expression<!> : T) : Out<out T> = Out<T>()

fun fooout() : Out<Point> {
  val p = Point();
  return fout<Point>(p);
}

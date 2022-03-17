class Point() {
}

class G<T>() {}

fun <T> f(expression : T) : G<out T> = G<T>()


fun foo() : G<Point> {
  val p = Point()
  return <!RETURN_TYPE_MISMATCH!>f<Point>(p)<!>
}

class Out<out T>() {}

fun <T> fout(expression : T) : Out<<!REDUNDANT_PROJECTION!>out<!> T> = Out<T>()

fun fooout() : Out<Point> {
  val p = Point();
  return fout<Point>(p);
}

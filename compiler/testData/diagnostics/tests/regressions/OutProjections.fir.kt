// !WITH_NEW_INFERENCE
class Point() {
}

class G<T>() {}

fun <T> f(expression : T) : G<out T> = G<T>()


fun foo() : G<Point> {
  val p = Point()
  return <!RETURN_TYPE_MISMATCH, TYPE_MISMATCH!>f<Point>(p)<!>
}

class Out<out T>() {}

fun <T> fout(expression : T) : Out<out T> = Out<T>()

fun fooout() : Out<Point> {
  val p = Point();
  return fout<Point>(p);
}

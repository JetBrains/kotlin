// !WITH_NEW_INFERENCE
class Point() {
}

class G<T>() {}

fun <T> f(<!UNUSED_PARAMETER!>expression<!> : T) : G<out T> = G<T>()


fun foo() : G<Point> {
  val p = Point()
  return <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>f<Point>(p)<!>
}

class Out<out T>() {}

fun <T> fout(<!UNUSED_PARAMETER!>expression<!> : T) : Out<<!REDUNDANT_PROJECTION!>out<!> T> = Out<T>()

fun fooout() : Out<Point> {
  val p = Point();
  return <!NI;TYPE_MISMATCH!>fout<Point>(p)<!>;
}
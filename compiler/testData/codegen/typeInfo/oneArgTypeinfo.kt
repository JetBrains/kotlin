class Point() {
}

fun foo() : TypeInfo<Point> {
  val p = Point();
  return typeinfo<Point>(p);
}

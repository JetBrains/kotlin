fun foo(i: Int): Int {
  var count = i;
  var result = 0;
  while(count > 0) {
    count = count - 1;
    if (count <= 2) continue;
    result = result + count;
  }
  return result;
}

fun box(): String {
  if (foo(4) != 3) return "Fail 1"
  if (foo(5) != 7) return "Fail 2"
  return "OK"
}

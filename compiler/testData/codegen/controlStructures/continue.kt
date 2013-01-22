fun continue_test(i: Int): Int {
  var count = i;
  var result = 0;
  while(count > 0) {
    count = count - 1;
    if (count <= 2) continue;
    result = result + count;
  }
  return result;
}

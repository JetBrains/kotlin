function box() {
  var i = 1;
  var sum = 0;
  outer: do {
    $l$loop: while (i < 10) {
      sum = sum + i | 0;
      i = i + 1 | 0;
      if (sum > 20)
        break outer;
    }
  }
   while (false);
  if (!(sum === 21))
    return 'fail: ' + sum;
  return 'OK';
}

function box() {
  var i;
  var sum = 0;
  var count = 2;
  $l$loop: while (true) {
    var _unary__edvuaz = count;
    count = _unary__edvuaz - 1 | 0;
    if (!(_unary__edvuaz > 0)) {
      break $l$loop;
    }
    i = 1;
    $l$loop_0: while (i < 10) {
      sum = sum + i | 0;
      i = i + 1 | 0;
    }
  }
  if (!(sum === 90))
    return 'fail: ' + sum;
  return 'OK';
}

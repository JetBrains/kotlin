function box() {
  let i;
  let sum = 0;
  let count = 2;
  $l$loop: while (true) {
    const _unary__edvuaz = count;
    count = _unary__edvuaz - 1 | 0;
    if (!(_unary__edvuaz > 0)) {
      break $l$loop;
    }
    i = 1;
    while (!(i >= 10)) {
      sum = sum + i | 0;
      i = i + 1 | 0;
    }
  }
  if (!(sum === 90))
    return 'fail: ' + sum;
  return 'OK';
}

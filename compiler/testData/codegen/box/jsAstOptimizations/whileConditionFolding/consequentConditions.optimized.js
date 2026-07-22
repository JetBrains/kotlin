function box() {
  var i = 1;
  var sum = 0;
  while (!(i >= 10) && !(sum > 30)) {
    sum = sum + i | 0;
    i = i + 1 | 0;
  }
  if (!(sum === 36))
    return 'fail: ' + sum;
  return 'OK';
}

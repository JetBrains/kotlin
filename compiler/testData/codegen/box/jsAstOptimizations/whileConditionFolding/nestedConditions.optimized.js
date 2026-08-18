function box() {
  var i = 1;
  var sum = 0;
  while (!(i >= 5) || !(sum > 40)) {
    sum = sum + i | 0;
    i = i + 1 | 0;
  }
  if (!(sum === 45))
    return 'fail: ' + sum;
  return 'OK';
}

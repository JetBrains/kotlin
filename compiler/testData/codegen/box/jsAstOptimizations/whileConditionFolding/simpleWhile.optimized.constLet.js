function box() {
  let i = 1;
  let sum = 0;
  while (!(i >= 10)) {
    sum = sum + i | 0;
    i = i + 1 | 0;
  }
  if (!(sum === 45))
    return 'fail: ' + sum;
  return 'OK';
}

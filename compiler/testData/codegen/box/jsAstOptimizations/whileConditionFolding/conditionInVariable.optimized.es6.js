function box() {
  let i = 0;
  let j = 0;
  let k = 0;
  let result = '';
  $l$loop: while (i < 3 && !(j > 2)) {
    let tmp;
    tmp = j - 1 | 0;
    const a = tmp;
    if (k > 2)
      break $l$loop;
    i = i + 1 | 0;
    j = j + 1 | 0;
    k = k + 1 | 0;
    result = result + ('a=' + a + ';');
  }
  if (!(result === 'a=-1;a=0;a=1;'))
    return 'fail: ' + result;
  return 'OK';
}

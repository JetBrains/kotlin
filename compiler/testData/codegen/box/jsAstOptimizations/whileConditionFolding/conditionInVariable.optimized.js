function box() {
  var i = 0;
  var j = 0;
  var k = 0;
  var result = '';
  $l$loop: while (i < 3 && !(j > 2)) {
    var tmp;
    tmp = j - 1 | 0;
    var a = tmp;
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

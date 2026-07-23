function box() {
  var i = 0;
  var j = 0;
  var k = 0;
  var result = '';
  $l$loop_0: while (i < 3) {
    var tmp;
    if (j > 2) {
      break $l$loop_0;
    } else {
      tmp = j - 1 | 0;
    }
    var a = tmp;
    if (k > 2)
      break $l$loop_0;
    i = i + 1 | 0;
    j = j + 1 | 0;
    k = k + 1 | 0;
    result = result + ('a=' + a + ';');
  }
  if (!(result === 'a=-1;a=0;a=1;'))
    return 'fail: ' + result;
  return 'OK';
}

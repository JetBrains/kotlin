function box() {
  var i = 0;
  var j = 0;
  var log = '';
  outer: while (i < 10) {
    while (j < 10) {
      if ((i + j | 0) >= 3) {
        break outer;
      }
      log = log + ('' + i + ',' + j + ';');
      j = j + 1 | 0;
    }
    i = i + 1 | 0;
  }
  if (!(log === '0,0;0,1;0,2;'))
    return 'fail: ' + log;
  return 'OK';
}

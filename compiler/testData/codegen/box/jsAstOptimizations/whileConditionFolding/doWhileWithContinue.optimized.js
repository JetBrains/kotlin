function box() {
  var i = 0;
  $l$loop_0: do {
    i = i + 1 | 0;
    if (i === 5) {
      continue $l$loop_0;
    }
    global = global + ';';
    if (foo(i) >= 10) {
      break $l$loop_0;
    }
  }
   while (true);
  if (!(global === ';1;2;3;4;6;7;8;9;10'))
    return 'fail: ' + global;
  return 'OK';
}

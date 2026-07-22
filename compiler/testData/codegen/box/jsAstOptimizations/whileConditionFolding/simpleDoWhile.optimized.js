function box() {
  var i = 0;
  $l$loop: do {
    i = i + 1 | 0;
    global = global + ';';
  }
   while (foo(i) < 10);
  if (!(global === ';1;2;3;4;5;6;7;8;9;10'))
    return 'fail: ' + global;
  return 'OK';
}

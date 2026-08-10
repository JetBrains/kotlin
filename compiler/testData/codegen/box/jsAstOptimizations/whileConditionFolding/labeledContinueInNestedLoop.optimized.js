function box() {
  var i = 0;
  loop: do {
    i = i + 1 | 0;
    global = global + ';';
    var inductionVariable = 0;
    if (inductionVariable < 2)
      do {
        var j = inductionVariable;
        inductionVariable = inductionVariable + 1 | 0;
        if (j === 1 && i === 2) {
          continue loop;
        }
        global = global + '-';
      }
       while (inductionVariable < 2);
    if (foo(i) >= 5) {
      break loop;
    }
  }
   while (true);
  if (!(global === ';--1;-;--3;--4;--5'))
    return 'fail: ' + global;
  return 'OK';
}

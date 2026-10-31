function box() {
  let i = 0;
  let j;
  do {
    i = i + 1 | 0;
    global = global + ';';
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    const indexedObject = ['a', 'b'];
    let inductionVariable = 0;
    const last = indexedObject.length;
    $l$loop: while (inductionVariable < last) {
      const k = indexedObject[inductionVariable];
      inductionVariable = inductionVariable + 1 | 0;
      if (!(k === 'a')) {
        continue $l$loop;
      }
      global = global + '@';
    }
    j = 0;
    $l$loop_1: while (true) {
      const _unary__edvuaz = j;
      j = _unary__edvuaz + 1 | 0;
      if (!(_unary__edvuaz < 2)) {
        break $l$loop_1;
      }
      if (j === 1) {
        continue $l$loop_1;
      }
      global = global + '$';
    }
    j = 0;
    let _unary__edvuaz_0;
    $l$1: do {
      $l$0: do {
        if (j === 1) {
          continue $l$0;
        }
        global = global + '#';
      }
       while (false);
      _unary__edvuaz_0 = j;
      j = _unary__edvuaz_0 + 1 | 0;
    }
     while (_unary__edvuaz_0 < 2);
  }
   while (!(foo(i) >= 3));
  if (!(global === ';@$##1;@$##2;@$##3'))
    return 'fail: ' + global;
  return 'OK';
}

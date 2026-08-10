function box() {
  var i = 0;
  var j;
  do {
    i = i + 1 | 0;
    global = global + ';';
    // Inline function 'kotlin.arrayOf' call
    // Inline function 'kotlin.js.unsafeCast' call
    // Inline function 'kotlin.js.asDynamic' call
    var indexedObject = ['a', 'b'];
    var inductionVariable = 0;
    var last = indexedObject.length;
    $l$loop: while (inductionVariable < last) {
      var k = indexedObject[inductionVariable];
      inductionVariable = inductionVariable + 1 | 0;
      if (!(k === 'a')) {
        continue $l$loop;
      }
      global = global + '@';
    }
    j = 0;
    $l$loop_1: while (true) {
      var _unary__edvuaz = j;
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
    $l$1: do {
      $l$0: do {
        if (j === 1) {
          continue $l$0;
        }
        global = global + '#';
      }
       while (false);
      var _unary__edvuaz_0 = j;
      j = _unary__edvuaz_0 + 1 | 0;
    }
     while (_unary__edvuaz_0 < 2);
  }
   while (!(foo(i) >= 3));
  if (!(global === ';@$##1;@$##2;@$##3'))
    return 'fail: ' + global;
  return 'OK';
}

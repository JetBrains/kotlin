if (typeof kotlin === 'undefined') {
  throw new Error("Error loading module 'sample'. Its dependency 'kotlin' was not found. Please, check whether 'kotlin' is loaded prior to 'sample'.");
}
var sample = function (_, Kotlin) {
  'use strict';
  var println = Kotlin.kotlin.io.println_s8jyv4$;
  function foo() {
    println('foo');
  }
  function bar() {
    println('bar');
  }
  function main(args) {
    foo();
  }
  _.foo = foo;
  _.bar = bar;
  _.main_kand9s$ = main;
  main([]);
  Kotlin.defineModule('sample', _);
  return _;
}(typeof sample === 'undefined' ? {} : sample, kotlin);

//# sourceMappingURL=sample.js.map

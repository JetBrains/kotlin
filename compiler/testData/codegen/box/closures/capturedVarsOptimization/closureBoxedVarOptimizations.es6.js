function captureVarInInlineLambda() {
  var any = new Object();
  var byte = 1;
  var short = 2;
  var int = 3;
  var long = 4n;
  var float = 5.0;
  var double = 6.0;
  var char = _Char___init__impl__6a9atx(97);
  var boolean = true;
  // Inline function 'run' call
  any = null;
  byte = 101;
  short = 102;
  int = 103;
  long = 104n;
  float = 105.0;
  double = 106.0;
  char = _Char___init__impl__6a9atx(98);
  boolean = false;
}
function captureVarInLocalClassInInlineLambda() {
  var any = {_v: new Object()};
  var byte = {_v: 1};
  var short = {_v: 2};
  var int = {_v: 3};
  var long = {_v: 4n};
  var float = {_v: 5.0};
  var double = {_v: 6.0};
  var char = {_v: new Char(_Char___init__impl__6a9atx(97))};
  var boolean = {_v: true};
  // Inline function 'run' call
  captureVarInLocalClassInInlineLambda$1.new__no_name_provided__cpb3g4_k$(any, byte, short, int, long, float, double, char, boolean).foo_i36xoq_k$();
}
function captureValueClassVar() {
  var any = {_v: new AnyWrapper(_AnyWrapper___init__impl__jd5khy(new Object()))};
  var byte = {_v: new ByteWrapper(_ByteWrapper___init__impl__bcpnw2(1))};
  var short = {_v: new ShortWrapper(_ShortWrapper___init__impl__bdjki2(2))};
  var int = {_v: new IntWrapper(_IntWrapper___init__impl__y2azvh(3))};
  var long = {_v: new LongWrapper(_LongWrapper___init__impl__et53uq(4n))};
  var float = {_v: new FloatWrapper(_FloatWrapper___init__impl__87f78q(5.0))};
  var double = {_v: new DoubleWrapper(_DoubleWrapper___init__impl__um2ad3(6.0))};
  var char = {_v: new CharWrapper(_CharWrapper___init__impl__iwctd0(_Char___init__impl__6a9atx(97)))};
  var boolean = {_v: new BooleanWrapper(_BooleanWrapper___init__impl__qaxidm(true))};
  run2(captureValueClassVar$lambda(any, byte, short, int, long, float, double, char, boolean));
}
function captureValueClassVar$lambda($any, $byte, $short, $int, $long, $float, $double, $char, $boolean) {
  return () => {
    $any._v = new AnyWrapper(_AnyWrapper___init__impl__jd5khy(null));
    $byte._v = new ByteWrapper(_ByteWrapper___init__impl__bcpnw2(101));
    $short._v = new ShortWrapper(_ShortWrapper___init__impl__bdjki2(102));
    $int._v = new IntWrapper(_IntWrapper___init__impl__y2azvh(103));
    $long._v = new LongWrapper(_LongWrapper___init__impl__et53uq(104n));
    $float._v = new FloatWrapper(_FloatWrapper___init__impl__87f78q(105.0));
    $double._v = new DoubleWrapper(_DoubleWrapper___init__impl__um2ad3(106.0));
    $char._v = new CharWrapper(_CharWrapper___init__impl__iwctd0(_Char___init__impl__6a9atx(98)));
    $boolean._v = new BooleanWrapper(_BooleanWrapper___init__impl__qaxidm(false));
    return Unit_getInstance();
  };
}

function captureVarInInlineLambda() {
  var any = new Object();
  var byte = 1;
  var short = 2;
  var int = 3;
  var long = new Long(4, 0);
  var float = 5.0;
  var double = 6.0;
  var char = Char$_Char___init__impl__o2jlrb(97);
  var boolean = true;
  // Inline function 'run' call
  any = null;
  byte = 101;
  short = 102;
  int = 103;
  long = new Long(104, 0);
  float = 105.0;
  double = 106.0;
  char = Char$_Char___init__impl__o2jlrb(98);
  boolean = false;
}
function captureVarInLocalClassInInlineLambda() {
  var any = {_v: new Object()};
  var byte = {_v: 1};
  var short = {_v: 2};
  var int = {_v: 3};
  var long = {_v: new Long(4, 0)};
  var float = {_v: 5.0};
  var double = {_v: 6.0};
  var char = {_v: new Char(Char$_Char___init__impl__o2jlrb(97))};
  var boolean = {_v: true};
  // Inline function 'run' call
  (new captureVarInLocalClassInInlineLambda$1(any, byte, short, int, long, float, double, char, boolean)).foo_i36xoq_k$();
}
function captureValueClassVar() {
  var any = {_v: new AnyWrapper(AnyWrapper$_AnyWrapper___init__impl__q96myx(new Object()))};
  var byte = {_v: new ByteWrapper(ByteWrapper$_ByteWrapper___init__impl__hjncq3(1))};
  var short = {_v: new ShortWrapper(ShortWrapper$_ShortWrapper___init__impl__y3la7d(2))};
  var int = {_v: new IntWrapper(IntWrapper$_IntWrapper___init__impl__400bnn(3))};
  var long = {_v: new LongWrapper(LongWrapper$_LongWrapper___init__impl__lpaad(new Long(4, 0)))};
  var float = {_v: new FloatWrapper(FloatWrapper$_FloatWrapper___init__impl__4j374n(5.0))};
  var double = {_v: new DoubleWrapper(DoubleWrapper$_DoubleWrapper___init__impl__wah81h(6.0))};
  var char = {_v: new CharWrapper(CharWrapper$_CharWrapper___init__impl__1n66az(Char$_Char___init__impl__o2jlrb(97)))};
  var boolean = {_v: new BooleanWrapper(BooleanWrapper$_BooleanWrapper___init__impl__iji4ep(true))};
  run2(captureValueClassVar$lambda(any, byte, short, int, long, float, double, char, boolean));
}
function captureValueClassVar$lambda($any, $byte, $short, $int, $long, $float, $double, $char, $boolean) {
  return function () {
    $any._v = new AnyWrapper(AnyWrapper$_AnyWrapper___init__impl__q96myx(null));
    $byte._v = new ByteWrapper(ByteWrapper$_ByteWrapper___init__impl__hjncq3(101));
    $short._v = new ShortWrapper(ShortWrapper$_ShortWrapper___init__impl__y3la7d(102));
    $int._v = new IntWrapper(IntWrapper$_IntWrapper___init__impl__400bnn(103));
    $long._v = new LongWrapper(LongWrapper$_LongWrapper___init__impl__lpaad(new Long(104, 0)));
    $float._v = new FloatWrapper(FloatWrapper$_FloatWrapper___init__impl__4j374n(105.0));
    $double._v = new DoubleWrapper(DoubleWrapper$_DoubleWrapper___init__impl__wah81h(106.0));
    $char._v = new CharWrapper(CharWrapper$_CharWrapper___init__impl__1n66az(Char$_Char___init__impl__o2jlrb(98)));
    $boolean._v = new BooleanWrapper(BooleanWrapper$_BooleanWrapper___init__impl__iji4ep(false));
    return Unit_getInstance();
  };
}

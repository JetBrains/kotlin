function captureVarInInlineLambda() {
  var any = new Object();
  var byte = 1;
  var short = 2;
  var int = 3;
  var long = 4n;
  var float = 5.0;
  var double = 6.0;
  var char = Char$__init__impl__fq95zl(97);
  var boolean = true;
  // Inline function 'run' call
  any = null;
  byte = 101;
  short = 102;
  int = 103;
  long = 104n;
  float = 105.0;
  double = 106.0;
  char = Char$__init__impl__fq95zl(98);
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
  var char = {_v: new Char(Char$__init__impl__fq95zl(97))};
  var boolean = {_v: true};
  // Inline function 'run' call
  captureVarInLocalClassInInlineLambda$1.new__no_name_provided__cpb3g4_k$(any, byte, short, int, long, float, double, char, boolean).foo_i36xoq_k$();
}
function captureValueClassVar() {
  var any = {_v: new AnyWrapper(AnyWrapper$__init__impl__qih0n2(new Object()))};
  var byte = {_v: new ByteWrapper(ByteWrapper$__init__impl__4fcome(1))};
  var short = {_v: new ShortWrapper(ShortWrapper$__init__impl__ibjnda(2))};
  var int = {_v: new IntWrapper(IntWrapper$__init__impl__8wtj1d(3))};
  var long = {_v: new LongWrapper(LongWrapper$__init__impl__lqi34e(4n))};
  var float = {_v: new FloatWrapper(FloatWrapper$__init__impl__f5fa3y(5.0))};
  var double = {_v: new DoubleWrapper(DoubleWrapper$__init__impl__wx5l6l(6.0))};
  var char = {_v: new CharWrapper(CharWrapper$__init__impl__byzu3c(Char$__init__impl__fq95zl(97)))};
  var boolean = {_v: new BooleanWrapper(BooleanWrapper$__init__impl__1oe9ja(true))};
  run2(captureValueClassVar$lambda(any, byte, short, int, long, float, double, char, boolean));
}
function captureValueClassVar$lambda($any, $byte, $short, $int, $long, $float, $double, $char, $boolean) {
  return () => {
    $any._v = new AnyWrapper(AnyWrapper$__init__impl__qih0n2(null));
    $byte._v = new ByteWrapper(ByteWrapper$__init__impl__4fcome(101));
    $short._v = new ShortWrapper(ShortWrapper$__init__impl__ibjnda(102));
    $int._v = new IntWrapper(IntWrapper$__init__impl__8wtj1d(103));
    $long._v = new LongWrapper(LongWrapper$__init__impl__lqi34e(104n));
    $float._v = new FloatWrapper(FloatWrapper$__init__impl__f5fa3y(105.0));
    $double._v = new DoubleWrapper(DoubleWrapper$__init__impl__wx5l6l(106.0));
    $char._v = new CharWrapper(CharWrapper$__init__impl__byzu3c(Char$__init__impl__fq95zl(98)));
    $boolean._v = new BooleanWrapper(BooleanWrapper$__init__impl__1oe9ja(false));
    return Unit$getInstance();
  };
}

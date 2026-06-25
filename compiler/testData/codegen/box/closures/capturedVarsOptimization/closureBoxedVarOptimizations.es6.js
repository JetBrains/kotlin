function captureVarInInlineLambda() {
  let any = new Object();
  let byte = 1;
  let short = 2;
  let int = 3;
  let long = 4n;
  let float = 5.0;
  let double = 6.0;
  let char = Char$__init__impl__fq95zl(97);
  let boolean = true;
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
  const any = {_v: new Object()};
  const byte = {_v: 1};
  const short = {_v: 2};
  const int = {_v: 3};
  const long = {_v: 4n};
  const float = {_v: 5.0};
  const double = {_v: 6.0};
  const char = {_v: new Char(Char$__init__impl__fq95zl(97))};
  const boolean = {_v: true};
  // Inline function 'run' call
  captureVarInLocalClassInInlineLambda$1.new__no_name_provided__cpb3g4_k$(any, byte, short, int, long, float, double, char, boolean).foo_i36xoq_k$();
}
function captureValueClassVar() {
  const any = {_v: new AnyWrapper(AnyWrapper$__init__impl__qih0n2(new Object()))};
  const byte = {_v: new ByteWrapper(ByteWrapper$__init__impl__4fcome(1))};
  const short = {_v: new ShortWrapper(ShortWrapper$__init__impl__ibjnda(2))};
  const int = {_v: new IntWrapper(IntWrapper$__init__impl__8wtj1d(3))};
  const long = {_v: new LongWrapper(LongWrapper$__init__impl__lqi34e(4n))};
  const float = {_v: new FloatWrapper(FloatWrapper$__init__impl__f5fa3y(5.0))};
  const double = {_v: new DoubleWrapper(DoubleWrapper$__init__impl__wx5l6l(6.0))};
  const char = {_v: new CharWrapper(CharWrapper$__init__impl__byzu3c(Char$__init__impl__fq95zl(97)))};
  const boolean = {_v: new BooleanWrapper(BooleanWrapper$__init__impl__1oe9ja(true))};
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

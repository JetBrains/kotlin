function topLevelWithoutFileInitializerCall(x) {
  x = x === VOID ? 3 : x;
  return 'x=' + x;
}
class C1 {
  constructor(s = 2) {
    return new.target.new_C1_yyvy5v_k$(s);
  }
  static new_C1_yyvy5v_k$(s = 2, $box) {
    var $this = createThis(this, $box);
    $this.s_1 = s;
    return $this;
  }
  get_s_1mhr62_k$() {
    return this.s_1;
  }
  static new_C1_fu22lr_k$(marker, t = 19, u = 20, $box) {
    return this.new_C1_yyvy5v_k$(t + u | 0, $box);
  }
  bar_y6pwbp_k$(x = 1) {
    return 'x=' + x;
  }
}

class C2 {
  constructor() {
    return new.target.new_C2_ejdxgx_k$();
  }
  static new_C2_ejdxgx_k$($box) {
    return createThis(this, $box);
  }
  bar_y6pwbp_k$(x) {
    return 'x=' + x;
  }
  bar$default_1ehwjv_k$(x = 12, $super) {
    return $super === VOID ? this.bar_y6pwbp_k$(x) : $super.bar_y6pwbp_k$.call(this, x);
  }
}

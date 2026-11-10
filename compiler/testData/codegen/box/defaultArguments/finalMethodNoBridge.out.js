function topLevelWithoutFileInitializerCall(x) {
  x = x === VOID ? 3 : x;
  return 'x=' + x;
}
class C1 {
  constructor(s) {
    return new.target.new_C1_todw5y_k$(s);
  }
  static new_C1_todw5y_k$(s, $box) {
    s = s === VOID ? 2 : s;
    var $this = createThis(this, $box);
    $this.s_1 = s;
    return $this;
  }
  get_s_1mhr62_k$() {
    return this.s_1;
  }
  static new_C1_dr2s71_k$(marker, t, u, $box) {
    t = t === VOID ? 19 : t;
    u = u === VOID ? 20 : u;
    return this.new_C1_todw5y_k$(t + u | 0, $box);
  }
  bar_y6pwbp_k$(x) {
    return 'x=' + x;
  }
  bar$default_t2ic8p_k$(x, $super) {
    x = x === VOID ? 1 : x;
    return $super === VOID ? this.bar_y6pwbp_k$(x) : $super.bar_y6pwbp_k$.call(this, x);
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
  bar$default_o7tp16_k$(x, $super) {
    x = x === VOID ? 12 : x;
    return $super === VOID ? this.bar_y6pwbp_k$(x) : $super.bar_y6pwbp_k$.call(this, x);
  }
}

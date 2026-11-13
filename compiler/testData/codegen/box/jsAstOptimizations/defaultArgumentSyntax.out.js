function withAnonymousObject(producer1 = new withAnonymousObject$1(), producer2 = new withAnonymousObject$2()) {
  return producer1.produce() + ' ' + producer2.produce();
}
function triggeringTopLevelConstInitializer(x = theAnswer, y, z = theAnswer) {
  return 'x=' + x + ', y=' + y + ', z=' + z;
}
function triggeringTopLevelPureInitializer(x, y, z) {
  _init_properties_topLevelPureInitializer_kt__q513wp();
  x = x === VOID ? get_theAnswer() : x;
  z = z === VOID ? get_theAnswer() : z;
  return 'x=' + x + ', y=' + y + ', z=' + z;
}
function triggeringTopLevelEffectfulInitializer(x, y, z) {
  _init_properties_topLevelEffectfulInitializer_kt__q0577p();
  x = x === VOID ? get_theAnswerWithEffect() : x;
  z = z === VOID ? get_theAnswerWithEffect() : z;
  return 'x=' + x + ', y=' + y + ', z=' + z;
}
function referencingOtherParameters(x, y = x + 1 | 0, z = imul(y, 2)) {
  return 'x=' + x + ', y=' + y + ', z=' + z;
}
function simpleConditional(condition, x = condition ? 1 : -1) {
  return 'x=' + x;
}
function complexConditional(x, y) {
  var tmp;
  if (y === VOID) {
    var tmp_0;
    if (x === 1) {
      set_log(get_log() + 'complexConditional;');
      tmp_0 = 0;
    } else {
      tmp_0 = x + 1 | 0;
    }
    tmp = tmp_0;
  } else {
    tmp = y;
  }
  y = tmp;
  return 'x=' + x + ', y=' + y;
}
class Animal {
  say(prefix = this.defaultPrefix, $super) {
    return $super === VOID ? this.n(prefix) : $super.n.call(this, prefix);
  }
  get defaultPrefix() {
    return this.m();
  }
}

class Cat extends Animal {
  m() {
    return '';
  }
  n(prefix) {
    return prefix + 'nya!';
  }
}


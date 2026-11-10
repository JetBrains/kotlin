function withAnonymousObject(producer1, producer2) {
  var tmp;
  if (producer1 === VOID) {
    tmp = new withAnonymousObject$1();
  } else {
    tmp = producer1;
  }
  producer1 = tmp;
  var tmp_0;
  if (producer2 === VOID) {
    tmp_0 = new withAnonymousObject$2();
  } else {
    tmp_0 = producer2;
  }
  producer2 = tmp_0;
  return producer1.produce() + ' ' + producer2.produce();
}
function triggeringTopLevelConstInitializer(x, y, z) {
  x = x === VOID ? theAnswer : x;
  z = z === VOID ? theAnswer : z;
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
function referencingOtherParameters(x, y, z) {
  y = y === VOID ? x + 1 | 0 : y;
  z = z === VOID ? imul(y, 2) : z;
  return 'x=' + x + ', y=' + y + ', z=' + z;
}
function simpleConditional(condition, x) {
  x = x === VOID ? condition ? 1 : -1 : x;
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
  say(prefix, $super) {
    prefix = prefix === VOID ? this.defaultPrefix : prefix;
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


function box() {
  sep('Simple call');
  // Inline function 'inlineFunction' call
  // Inline function 'inlineFunction' call
  setOK();
  sep('Call in if');
  if (!(flag1 === 0)) {
    var tmp;
    var tmp_0;
    if (equals(OK, 'OK') && flag1 === 1) {
      tmp_0 = typeof flag2 === 'number';
    } else {
      tmp_0 = false;
    }
    if (tmp_0) {
      tmp = check_0();
    } else {
      tmp = false;
    }
    if (tmp) {
      // Inline function 'inlineFunction' call
    }
  }
  sep('Call in else');
  if (!(flag1 === 0)) {
    var tmp_1;
    var tmp_2;
    if (equals(OK, 'OK') && flag1 === 1) {
      tmp_2 = typeof flag2 === 'number';
    } else {
      tmp_2 = false;
    }
    if (tmp_2) {
      tmp_1 = check_0();
    } else {
      tmp_1 = false;
    }
    if (tmp_1) {
      check_0();
      check_0();
    } else {
      // Inline function 'inlineFunction' call
    }
  }
  sep('Call in while');
  while (!equals(OK, 'OK')) {
    // Inline function 'inlineFunction' call
  }
  sep('Call in when');
  var tmp0_subject = OK;
  if (!(tmp0_subject == null) && typeof tmp0_subject === 'string') {
    // Inline function 'inlineFunction' call
  } else {
    if (isNumber(tmp0_subject)) {
      // Inline function 'inlineFunction' call
    } else {
      // Inline function 'inlineFunction' call
    }
  }
  sep('Call in try/catch/finally');
  try {
    // Inline function 'inlineFunction' call
  } catch ($p) {
    if ($p instanceof Exception) {
      var e = $p;
      // Inline function 'inlineFunction' call
    } else {
      throw $p;
    }
  }
  finally {
    // Inline function 'inlineFunction' call
  }
  sep('End');
  var tmp_3 = ensureNotNull(OK);
  return typeof tmp_3 === 'string' ? tmp_3 : THROW_CCE();
}

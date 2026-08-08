function box() {
  sep('Simple call');
  // Inline function 'inlineFunction' call
  // Inline function 'inlineFunction' call
  setOK();
  sep('Call in if');
  if (!(flag1 === 0)) {
    let tmp;
    let tmp_0;
    if (equals(OK, 'OK') && flag1 === 1) {
      const tmp_1 = flag2;
      tmp_0 = typeof tmp_1 === 'number';
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
    let tmp_2;
    let tmp_3;
    if (equals(OK, 'OK') && flag1 === 1) {
      const tmp_4 = flag2;
      tmp_3 = typeof tmp_4 === 'number';
    } else {
      tmp_3 = false;
    }
    if (tmp_3) {
      tmp_2 = check_0();
    } else {
      tmp_2 = false;
    }
    if (tmp_2) {
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
  const tmp0_subject = OK;
  if (!(tmp0_subject == null) ? typeof tmp0_subject === 'string' : false) {
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
      const e = $p;
      // Inline function 'inlineFunction' call
    } else {
      throw $p;
    }
  }
  finally {
    // Inline function 'inlineFunction' call
  }
  sep('End');
  const tmp_5 = OK;
  return (!(tmp_5 == null) ? typeof tmp_5 === 'string' : false) ? tmp_5 : THROW_CCE();
}

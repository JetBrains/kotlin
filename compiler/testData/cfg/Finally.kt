fun t1() {
  try {
    1
  } finally {
    2
  }
}

fun t2() {
  try {
    1
    if (2 > 3) {
      return
    }
  } finally {
    2
  }
}

fun t3() {
  try {
    1
    @{ () ->
        if (2 > 3) {
          return@
        }
    }
  } finally {
    2
  }
}

fun t4() {
  @{ () ->
    try {
      1
      if (2 > 3) {
        return@
      }
    } finally {
      2
    }
  }
}

fun t5() {
  @ while(true) {
    try {
      1
      if (2 > 3) {
        break @
      }
    } finally {
      2
    }
  }
}

fun t6() {
  try {
    @ while(true) {
        1
        if (2 > 3) {
          break @
        }
      }
      5
  } finally {
    2
  }
}

fun t7() {
  try {
    @ while(true) {
        1
        if (2 > 3) {
          break @
        }
      }
  } finally {
    2
  }
}

fun t8(a : Int) {
  @ for (i in 1..a) {
    try {
      1
      if (2 > 3) {
        continue @
      }
    } finally {
      2
    }
  }
}

fun t9(a : Int) {
  try {
    @ for (i in 1..a) {
        1
        if (2 > 3) {
          continue @
        }
      }
      5
  } finally {
    2
  }
}

fun t10(a : Int) {
  try {
    @ for (i in 1..a) {
        1
        if (2 > 3) {
          continue @
        }
      }
  } finally {
    2
  }
}

fun t11() {
  try {
    return 1
  }
  finally {
    return 2
  }
}

fun t12() : Int {
    try {
        return 1
    }
    finally {
        doSmth(3)
    }
}

fun t13() : Int {
    try {
        return 1
    }
    catch (e: UnsupportedOperationException) {
        doSmth(2)
    }
    finally {
        doSmth(3)
    }
}

fun t14() : Int {
    try {
        return 1
    }
    catch (e: UnsupportedOperationException) {
        doSmth(2)
    }
}


fun t15() : Int {
    try {
        return 1
    }
    catch (e: UnsupportedOperationException) {
        return 2
    }
    finally {
        doSmth(3)
    }
}

fun t16() : Int {
    try {
        doSmth(1)
    }
    catch (e: UnsupportedOperationException) {
        return 2
    }
    finally {
        doSmth(3)
    }
}

fun doSmth(i: Int) {
}
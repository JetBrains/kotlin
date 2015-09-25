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
    l@{ ->
        if (2 > 3) {
          return@l
        }
    }
  } finally {
    2
  }
}

fun t4() {
  l@{ ->
    try {
      1
      if (2 > 3) {
        return@l
      }
    } finally {
      2
    }
  }
}

fun t5() {
  l@ while(true) {
    try {
      1
      if (2 > 3) {
        break@l
      }
    } finally {
      2
    }
  }
}

fun t6() {
  try {
    l@ while(true) {
        1
        if (2 > 3) {
          break@l
        }
      }
      5
  } finally {
    2
  }
}

fun t7() {
  try {
    l@ while(true) {
        1
        if (2 > 3) {
          break@l
        }
      }
  } finally {
    2
  }
}

fun t8(a : Int) {
  l@ for (i in 1..a) {
    try {
      1
      if (2 > 3) {
        continue@l
      }
    } finally {
      2
    }
  }
}

fun t9(a : Int) {
  try {
    l@ for (i in 1..a) {
        1
        if (2 > 3) {
          continue@l
        }
      }
      5
  } finally {
    2
  }
}

fun t10(a : Int) {
  try {
    l@ for (i in 1..a) {
        1
        if (2 > 3) {
          continue@l
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
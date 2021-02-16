// FIR_COMPARISON
val a: Int = 1
    get() {
      return field
    }
    set(v) {
      field = <caret>
    }


// EXIST: a
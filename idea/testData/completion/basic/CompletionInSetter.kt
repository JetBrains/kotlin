val a: Int = 1
    get() {
      return $a
    }
    set(v) {
      $a = <caret>
    }


// EXIST: a
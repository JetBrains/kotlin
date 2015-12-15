// "Delete redundant extension property" "false"
// ACTION: Create test

var Thread.<caret>priority: Int
  get() = getPriority() + 1
  set(value) {
      setPriority(value)
  }
// "Delete redundant extension property" "false"

var Thread.<caret>priority: Int
  get() = getPriority() + 1
  set(value) {
      setPriority(value)
  }
// ERROR: Property must be initialized or be abstract
// WITH_RUNTIME
// SKIP_ERRORS_AFTER
class Owner {
    var <caret>p: Int
      get
      set
}

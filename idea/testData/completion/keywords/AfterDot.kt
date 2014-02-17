fun foo() {
    val str : String
    str.<caret>
}

// ABSENT: package, as, type, class, this, super, val, var, fun, for, null, true
// ABSENT: false, is, in, throw, return, break, continue, object, if, try, else, while
// ABSENT: do, when, trait, This


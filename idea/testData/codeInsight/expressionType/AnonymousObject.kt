fun baz() = run {
    val a = object {}
    <caret>a // empty box
}

// TYPE: a -> <html>&lt;anonymous object&gt;</html>

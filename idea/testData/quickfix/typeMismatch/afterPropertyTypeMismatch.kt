// "Change 'f' type to '(Long) -> Unit'" "true"
fun foo() {
    var f: (Long) -> Unit = if (true) { (x: Long) ->  }<caret> else { (x: Long) ->  }
}
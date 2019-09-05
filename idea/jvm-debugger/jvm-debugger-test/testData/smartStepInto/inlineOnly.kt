fun foo() {
    val a = mutableListOf("A", "B").also { it.add("C") }<caret>
    val b = a
}

// EXISTS: mutableListOf(vararg String)
// EXISTS: also: block.invoke()
class Outer {
    inner class Inner1
    inner class Inner2(v: String)
}

// 1 checkNotNullParameter
// 0 checkParameterIsNotNull
// 1 INVOKESTATIC

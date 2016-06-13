// "Surround with null check" "true"

fun foo(x: String?) {
    bar(<caret>x)
}

fun bar(s: String) = s.hashCode()
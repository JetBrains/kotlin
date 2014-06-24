fun box(): String {
    val supertypes = JavaClass.foo { a, b -> a.compareTo(b) }
    if (supertypes != "[java.util.Comparator<java.lang.String>]") return "Fail: $supertypes"
    return "OK"
}

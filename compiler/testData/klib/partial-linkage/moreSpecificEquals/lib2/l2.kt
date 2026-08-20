fun removedEqualityBound() = A("1") == A("1")
fun addedEqualityBound() = B("2") == B("2")
fun differentClasses() = B("3") == A("3") || A("3") == B("3")
fun changedEqualityBound() = C("4") == C("4")

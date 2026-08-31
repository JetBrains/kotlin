fun removedEqualityBound() = A("1") == A("1")
fun addedEqualityBound() = B("2") == B("2")
fun differentClasses() = B("3") == A("3") || A("3") == B("3")
fun changedEqualityBound() = C("4") == C("4")

class D(override val x: String) : WithNewBase(x) {
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean = x == other.x
}

fun changedHierarchyEquals() = D("5") == D("5")

open class WithRemovedAlias(x: String) : AliasTarget(x) {
    override fun equals(@EqualityBound(RemovedAlias::class) other: Any?): Boolean = x == other.x
}

open class WithChangedAlias(x: String) : AliasTarget(x) {
    override fun equals(@EqualityBound(ChangedAlias::class) other: Any?): Boolean = x == other.x
}

fun removedAliasEquals() = WithRemovedAlias("6") == WithRemovedAlias("6")
fun changedAliasEquals() = WithChangedAlias("7") == WithChangedAlias("7")

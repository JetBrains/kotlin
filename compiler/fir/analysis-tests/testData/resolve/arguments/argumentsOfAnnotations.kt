annotation class Ann(val x: Int, val y: String, val z: String = "z")

@Ann(y = "y", x = 10)
class A

annotation class AnnVarargs(val x: Int, vararg val y: String, val z: Int)

<!INAPPLICABLE_CANDIDATE!>@AnnVarargs(1, "a", "b", "c", 2)<!>
class B

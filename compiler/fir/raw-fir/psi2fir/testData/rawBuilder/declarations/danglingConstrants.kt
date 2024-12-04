fun function(): Int where T : @Anno("function contraint") List<@Anno("nested function contraint") Collection<@Anno("nested nested function contraint") String>> {}
val property: String where T : @Anno("property contraint") List<@Anno("nested property contraint") Collection<@Anno("nested nested property contraint") String>> =
    "0"

class TopLevelClass where T : @Anno("class contraint") List<@Anno("nested class contraint") Collection<@Anno("nested nested class contraint") String>> {
    fun memberFunction(): Int where T : @Anno("member function contraint") List<@Anno("nested member function contraint") Collection<@Anno("nested nested member function contraint") String>> {}
    val memberProperty: String where T : @Anno("member property contraint") List<@Anno("nested member property contraint") Collection<@Anno("nested nested member property contraint") String>> =
        "0"

    class NestedClass where T : @Anno("class contraint") List<@Anno("nested class contraint") Collection<@Anno("nested nested class contraint") String>>
}

data class TestClass1(@java.lang.Deprecated val pctorfield: Int) {
    constructor(@java.lang.Deprecated param: Int, param2: String) : this(param)
}

class TestClass2(
    @param:java.lang.Deprecated val deprecatedParamField: Int,
    @field:java.lang.Deprecated val deprecatedField: Int,
    @java.lang.Deprecated constructorParam: Int
) {
    fun foo(@java.lang.Deprecated functionParam) {}
}

// ANNOTATION: java.lang.Deprecated
// SEARCH: field:deprecatedField
// SEARCH: field:deprecatedParamField
// SEARCH: field:pctorfield
// SEARCH: method:component1
// SEARCH: method:getDeprecatedField
// SEARCH: method:getDeprecatedParamField
// SEARCH: method:getPctorfield
// SEARCH: param:constructorParam
// SEARCH: param:deprecatedField
// SEARCH: param:deprecatedParamField
// SEARCH: param:pctorfield
// SEARCH: param:functionParam
// SEARCH: param:param

class A(val OK: Int, val somePropertyWithLongName: String) {
    fun foo() {}
}
val topLevelProp = 1

const val propertyName1 = A::OK.name
const val propertyName2 = A::somePropertyWithLongName.name
const val methodName = A::foo.name
const val className = ::A.name
const val topLevelPropName = ::topLevelProp.name

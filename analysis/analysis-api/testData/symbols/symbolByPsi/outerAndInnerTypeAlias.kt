// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE
class X<T>

typealias TopLevelAlias = X<Int>

class A {
    typealias NestedLevelAlias = X<String>
    fun check() {
        typealias LocalTypeAlias = X<String>
        class LocalClass {
            typealias NestedLevelAliasInLocalClass = X<String>

            fun check() {
                typealias LocalTypeAlias = X<String>
                class LocalClass {
                    typealias NestedLevelAliasInLocalClass = X<String>
                    fun check() {

                    }
                }
            }
        }
    }
}

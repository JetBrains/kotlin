open class AliasHolder {
    typealias NestedAlias = String
}

typealias AliasToTopLevelClassInsideNested = AliasHolder.NestedAlias

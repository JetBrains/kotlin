// IGNORE_TREE_ACCESS: KT-64898
private val nonLocalProperty: List<XXX> by lazy {
    val localProperty = mutableListOf<KtLightField>()
    localProperty
}
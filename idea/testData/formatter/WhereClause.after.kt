abstract class RustNavigationContributorBase<T> protected constructor(
        private val indexKey: StubIndexKey<String, T>,
        private val clazz: Class<T>
) : ChooseByNameContributor, GotoClassContributor
        where T : NavigationItem,
              T : RustNamedElement {
}

fun <T> foo()
        where T : NavigationItem,
              T : RustNamedElement {
}

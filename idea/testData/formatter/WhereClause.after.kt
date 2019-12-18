abstract class RustNavigationContributorBase<T> protected constructor(
        private val indexKey: StubIndexKey<String, T>,
        private val clazz: Class<T>,
) : ChooseByNameContributor, GotoClassContributor
        where T : NavigationItem,
              T : RustNamedElement {
}

fun <T> foo()
        where T : NavigationItem,
              T : RustNamedElement {
}

interface Bound1
interface Bound2
class WhereClass1<T> where T : Bound1, T : Bound2

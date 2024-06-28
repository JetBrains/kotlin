// FIR_IDENTICAL
// ISSUE: KT-63578

abstract class Nav {
    val name: String = ""
}

object VerticalPortalTransitionProvider : SimplePortalTransitionProvider {
    override val enterTransition: String = ""
}

interface SimplePortalTransitionProvider {
    val enterTransition: String
}

internal abstract class BaseFeaturePortal<K> : SimplePortalTransitionProvider by VerticalPortalTransitionProvider

internal class FeaturePortal : BaseFeaturePortal<Nav>()

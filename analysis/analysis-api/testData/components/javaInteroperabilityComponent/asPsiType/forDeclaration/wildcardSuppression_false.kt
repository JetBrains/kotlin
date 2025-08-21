// WITH_STDLIB

interface NavGraphBuilder

interface AnimatedContentTransitionScope<S>

interface NavBackStackEntry

interface EnterTransition

fun NavGraphBuilder.compose(
  enter<caret>Transition: (@JvmSuppressWildcards(suppress = false)
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
) = TODO()
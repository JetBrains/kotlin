// WITH_STDLIB

interface NavGraphBuilder

interface AnimatedContentTransitionScope<S>

interface NavBackStackEntry

interface EnterTransition

fun NavGraphBuilder.compose(
  enter<caret>Transition: (@JvmSuppressWildcards(suppress = true)
    AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
) = TODO()
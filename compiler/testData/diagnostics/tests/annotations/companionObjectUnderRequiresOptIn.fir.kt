// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82524

@RequiresOptIn
annotation class ExperimentalForTest

class WithMarkedCompanion {
    @ExperimentalForTest
    companion object
}

@ExperimentalForTest
class WithMarkedOuter {
    companion object
}

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class ExperimentalForTestWithWarning

class WithMarkedCompanionWarning {
    @ExperimentalForTestWithWarning
    companion object
}

class WithExperimentalStdlib {
    @ExperimentalStdlibApi
    companion object
}

typealias WithMarkedCompanionTypealias = WithMarkedCompanion

fun test() {
    val withMarkedCompanion = WithMarkedCompanion
    val withMarkedOuter = <!OPT_IN_USAGE_ERROR!>WithMarkedOuter<!>
    val withMarkedCompanionWarning = WithMarkedCompanionWarning
    val withExperimentalStdlibApi = WithExperimentalStdlib
    val withMarkedCompanionViaTypealias = WithMarkedCompanionTypealias
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, functionDeclaration, localProperty,
objectDeclaration, propertyDeclaration */

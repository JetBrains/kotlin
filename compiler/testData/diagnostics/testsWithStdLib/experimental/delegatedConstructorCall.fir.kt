// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-45730

@RequiresOptIn
annotation class MyInternal

abstract class BaseClass @MyInternal constructor()

class Subclass @MyInternal constructor(): BaseClass()
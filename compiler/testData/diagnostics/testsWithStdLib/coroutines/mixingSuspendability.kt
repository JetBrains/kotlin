// FIR_IDENTICAL
interface AsyncVal { suspend fun getVal(): Int = 1}
interface SyncVal { fun getVal(): Int = 1 }

<!CONFLICTING_INHERITED_MEMBERS!>class MixSuspend<!> : AsyncVal, SyncVal {

}

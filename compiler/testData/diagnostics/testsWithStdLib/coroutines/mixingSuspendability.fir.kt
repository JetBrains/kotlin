// RUN_PIPELINE_TILL: FRONTEND

interface AsyncVal { suspend fun getVal(): Int = 1}
interface SyncVal { fun getVal(): Int = 1 }

<!CONFLICTING_INHERITED_MEMBERS, MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class MixSuspend<!> : AsyncVal, SyncVal {

}
